pipeline {
    agent any

    // Jenkins Job 실행 시 수동으로 전체 재배포를 강제할 수 있는 파라미터
    parameters {
        booleanParam(defaultValue: true, description: 'Force full deployment of user-admin-repo (ignores changes).', name: 'FORCE_FULL_DEPLOY')
    }

    environment {
        // NAS 환경에 맞는 Docker Registry와 Kubeconfig 경로 설정
        DOCKER_REGISTRY = 'localhost:5000' 
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        KUBECONFIG_PATH = '/var/lib/jenkins/.kube/config' // NAS 환경의 kubeconfig 경로
        NAMESPACE = 'bit-2503' // Kubernetes deployment.yaml의 네임스페이스와 일치
        
        // 배포 필요 여부를 저장할 플래그
        SHOULD_DEPLOY_USER_ADMIN_REPO = 'false'
    }

    // GitHub 웹훅 트리거 (deployNAS 브랜치에 푸시 시)
    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                // 사용자 정보를 바탕으로 브랜치와 크리덴셜 ID 설정
                // !!! 중요: user-admin-repo의 실제 GitHub Repository URL로 변경 필요 !!!
                git branch: 'deployNAS', credentialsId: 'github-token-333', url: 'https://github.com/jigubangbang/user-admin-repo.git' // user-admin-repo의 가정한 레포지토리 URL
            }
        }

        stage('Cleanup Failed Resources') {
            steps {
                script {
                    echo "=== 실패한 리소스 정리 ==="
                    sh """
                        export KUBECONFIG=${env.KUBECONFIG_PATH}
                        
                        echo "Cleaning up failed ReplicaSets..."

                        kubectl get rs -n ${env.NAMESPACE} --no-headers | awk '\$2 == 0 && \$3 == 0 && \$4 == 0 {print \$1}' | xargs -r kubectl delete rs -n ${env.NAMESPACE}
                        
                        echo "Cleaning up failed Pods..."

                        kubectl get pods -n ${env.NAMESPACE} --field-selector=status.phase=Failed -o name | xargs -r kubectl delete -n ${env.NAMESPACE}
                        kubectl get pods -n ${env.NAMESPACE} --field-selector=status.phase=Error -o name | xargs -r kubectl delete -n ${env.NAMESPACE}
                        
                        echo "Cleaning up old Docker images for user-admin-repo..."

                        docker rmi \$(docker images ${env.DOCKER_REGISTRY}/user-admin-repo --format "{{.Repository}}:{{.Tag}}" | grep -v ":${env.IMAGE_TAG}\\|:latest" | head -5) 2>/dev/null || true
                        
                        echo "Current resource usage:"
                        kubectl get resourcequota -n ${env.NAMESPACE} || echo "No resource quota found"
                        kubectl get pods -n ${env.NAMESPACE}
                    """
                }
            }
        }

        stage('Determine Changes') {
            steps {
                script {
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Git Commit: ${env.GIT_COMMIT}"
                    echo "Generated IMAGE_TAG: ${env.IMAGE_TAG}"

                    // FORCE_FULL_DEPLOY 파라미터 또는 첫 빌드 시 항상 배포
                    if (params.FORCE_FULL_DEPLOY || env.BUILD_NUMBER == '1') {
                        env.SHOULD_DEPLOY_USER_ADMIN_REPO = 'true'
                        echo "Force full deployment or first build - deploying user-admin-repo."
                    } else {
                        // 변경 사항 감지 (단일 레포이므로 단순히 변경이 있으면 배포)
                        def rawChanges = sh(returnStdout: true, script: 'git diff --name-only HEAD HEAD^1 || true').trim()
                        if (!rawChanges.isEmpty()) {
                            env.SHOULD_DEPLOY_USER_ADMIN_REPO = 'true'
                            echo "Detected changes in user-admin-repo repository. Deploying."
                        } else {
                            env.SHOULD_DEPLOY_USER_ADMIN_REPO = 'false'
                            echo "No changes detected in user-admin-repo. Skipping deployment."
                            currentBuild.result = 'SUCCESS'
                            return // 변경 사항이 없으면 파이프라인 종료
                        }
                    }
                }
            }
        }

        stage('Deploy User-Admin-Repo Bundle') {
            when {
                expression { return env.SHOULD_DEPLOY_USER_ADMIN_REPO == 'true' }
            }
            steps {
                script {
                    def serviceName = 'user-admin-repo'
                    def imageUrl = "${env.DOCKER_REGISTRY}/${serviceName}:${env.IMAGE_TAG}"

                    // Config Server가 준비될 때까지 기다림
                    echo "Waiting for Config Server to be ready before deploying ${serviceName}..."
                    retry(5) { // 불안정할 경우를 대비하여 재시도 횟수 증가
                        sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl wait --for=condition=available deployment/config-server-deployment -n ${env.NAMESPACE} --timeout=600s || exit 1"
                    }
                    echo "Config Server is ready."

                    // Eureka Server가 준비될 때까지 기다림
                    echo "Waiting for Eureka Server to be ready before deploying ${serviceName}..."
                    retry(5) { // 불안정할 경우를 대비하여 재시도 횟수 증가
                        sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl wait --for=condition=available deployment/eureka-server-deployment -n ${env.NAMESPACE} --timeout=600s || exit 1"
                    }
                    echo "Eureka Server is ready. Proceeding with ${serviceName} bundle."

                    echo "--- Building Docker image: ${imageUrl} ---"
                    // Dockerfile에 ARG가 없으므로 '--build-arg' 옵션 제거
                    sh """
                        docker build -t ${imageUrl} \\
                        ${params.FORCE_FULL_DEPLOY ? '--no-cache' : ''} .
                        docker push ${imageUrl}
                    """
                    echo "--- Docker image pushed: ${imageUrl} ---"

                    // --- 기존 배포 강제 삭제 (새로운 이미지의 클린한 롤아웃을 보장하기 위해) ---
                    echo "--- Existing deployment cleanup (to ensure clean rollout) ---"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl delete deployment ${serviceName}-deployment -n ${env.NAMESPACE} --ignore-not-found || true"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl wait --for=delete deployment/${serviceName}-deployment -n ${env.NAMESPACE} --timeout=300s --for=delete || true"
                    echo "--- Existing deployment deleted (if it existed) ---"

                    echo "--- Creating/Updating OAuth Secrets from Jenkins Credentials ---"
                    // Jenkins Credentials에서 값을 가져와 Kubernetes Secret을 생성/업데이트
                    withCredentials([string(credentialsId: 'jenkins-oauth-credentials', variable: 'OAUTH_CREDENTIALS_JSON')]) {
                        sh """
                            kubectl delete secret oauth-secrets -n ${env.NAMESPACE} --ignore-not-found || true
                            
                            KUBECONFIG=${env.KUBECONFIG_PATH} kubectl create secret generic oauth-secrets \
                                --namespace=${env.NAMESPACE} \
                                --from-literal=KAKAO_CLIENT_ID="${credentials('KAKAO_CLIENT_ID')}" \
                                --from-literal=GOOGLE_CLIENT_ID="${credentials('GOOGLE_CLIENT_ID')}" \
                                --from-literal=GOOGLE_CLIENT_SECRET="${credentials('GOOGLE_CLIENT_SECRET')}" \
                                --from-literal=NAVER_CLIENT_ID="${credentials('NAVER_CLIENT_ID')}" \
                                --from-literal=NAVER_CLIENT_SECRET="${credentials('NAVER_CLIENT_SECRET')}"
                        """
                    }
                    echo "--- OAuth Secrets are set ---"

                    // Kubernetes 배포 (NAS 환경)
                    echo "--- Applying Kubernetes deployments for ${serviceName} ---"
                    sh """
                        export KUBECONFIG=${env.KUBECONFIG_PATH}
                        

                        sed -i "s|__ECR_IMAGE_FULL_PATH__|${imageUrl}|g" k8s/deployment.yaml
                        
                        kubectl apply -f k8s/deployment.yaml -n ${env.NAMESPACE}
                        kubectl apply -f k8s/service.yaml -n ${env.NAMESPACE}

                    """
                    echo "--- Kubernetes deployments applied for ${serviceName} ---"

                    // --- Kubernetes Deployment Debugging (user-admin-repo 파드 관련) ---
                    echo "--- Kubernetes Deployment Debugging (${serviceName} Pods) ---"
                    echo "Pods in namespace ${env.NAMESPACE} after apply:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n ${env.NAMESPACE} -l app=${serviceName} || true"
                    echo "Deployment events:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl describe deployment/${serviceName}-deployment -n ${env.NAMESPACE} || true"

                    echo "Main container logs:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n ${env.NAMESPACE} -l app=${serviceName} -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- Main container {} logs: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n ${env.NAMESPACE} -c ${serviceName}-container || true; echo \"\";' || true"

                    echo "Init container logs (wait-for-config-server):"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n ${env.NAMESPACE} -l app=${serviceName} -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- Init container (config-server) {} logs: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n ${env.NAMESPACE} -c wait-for-config-server || true; echo \"\";' || true"

                    echo "Init container logs (wait-for-eureka):"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n ${env.NAMESPACE} -l app=${serviceName} -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- Init container (eureka) {} logs: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n ${env.NAMESPACE} -c wait-for-eureka || true; echo \"\";' || true"

                    echo "--- End Kubernetes Deployment Debugging (${serviceName} Pods) ---"

                    // 롤아웃 상태 대기
                    sh """
                        KUBECONFIG=${env.KUBECONFIG_PATH} kubectl rollout status deployment/${serviceName}-deployment -n ${env.NAMESPACE} --timeout=600s || exit 1
                    """
                    echo "${serviceName} 배포 완료."
                }
            }
        }
        
        stage('Verify Deployment') { // 최종 배포 확인 함수
            steps {
                script {
                    echo "=== 배포 확인 ==="
                    sh """
                        export KUBECONFIG=${env.KUBECONFIG_PATH}
                        echo "Pods in namespace ${env.NAMESPACE}:"
                        kubectl get pods -n ${env.NAMESPACE} -l app=${serviceName}
                        echo "Services in namespace ${env.NAMESPACE}:"
                        kubectl get services -n ${env.NAMESPACE} -l app=${serviceName}
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs() // 워크스페이스 정리
        }
        failure {
            echo "❌ CI/CD Pipeline failed for user-admin-repo."
        }
        success {
            echo "✅ CI/CD Pipeline finished successfully for user-admin-repo."
        }
    }
}