pipeline {
    agent any

    // Jenkins Job 실행 시 수동으로 전체 재배포를 강제할 수 있는 파라미터
    parameters {
        booleanParam(defaultValue: true, description: 'Force full deployment of user-admin-repo (ignores changes).', name: 'FORCE_FULL_DEPLOY')
    }

    environment {
        // AWS 계정 및 리전 정보
        AWS_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID = '947625948810'

        // EKS 클러스터 정보
        EKS_CLUSTER_NAME = 'msa-eks-cluster'
        EKS_KUBECTL_ROLE_ARN = "arn:aws:iam::${AWS_ACCOUNT_ID}:role/JenkinsEKSDeployerRole"

        // Docker 이미지 태그 (Jenkins 빌드 번호 사용)
        IMAGE_TAG = "${env.BUILD_NUMBER}"

        // kubectl 설정 파일 경로
        KUBECONFIG_PATH = "${WORKSPACE}/kubeconfig"

        // ==================== 백엔드 SECRET 환경 변수 (Jenkins Credentials에서 로드) ====================
        OAUTH_KAKAO_CLIENT_ID       = credentials('KAKAO_CLIENT_ID')
        OAUTH_NAVER_CLIENT_ID       = credentials('NAVER_CLIENT_ID')
        OAUTH_NAVER_CLIENT_SECRET   = credentials('NAVER_CLIENT_SECRET')
        OAUTH_GOOGLE_CLIENT_ID      = credentials('GOOGLE_CLIENT_ID')
        OAUTH_GOOGLE_CLIENT_SECRET  = credentials('GOOGLE_CLIENT_SECRET')
    }

    // GitHub 웹훅 트리거 (deployAWS 브랜치에 푸시 시)
    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'deployAWS', credentialsId: 'github-user-admin-repo-pat', url: 'https://github.com/jigubangbang/user-admin-repo.git'
            }
        }

        stage('Set AWS Kubeconfig') {
            steps {
                script {
                    withCredentials([aws(credentialsId: 'aws-cicd-credentials')]) {
                        sh "aws eks update-kubeconfig --name ${env.EKS_CLUSTER_NAME} --region ${env.AWS_REGION} --kubeconfig ${env.KUBECONFIG_PATH} --role-arn ${env.EKS_KUBECTL_ROLE_ARN}"
                    }
                }
            }
        }

        stage('Deploy User-Admin-Repo Bundle') {
            steps {
                script {
                    retry(3) { // 불안정한 네트워크 상황 대비 재시도
                        sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl wait --for=condition=available deployment/config-server-deployment -n default --timeout=600s || exit 1"
                    }
                    echo "Config Server is ready."

                    // --- Config Server 상태 상세 디버깅 시작 (user-admin-repo 배포 전) ---
                    echo "--- Config Server 상태 상세 디버깅 시작 ---"
                    echo "Config Server 파드 목록:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n default -l app=config-server || true"
                    echo "Config Server 배포 이벤트 확인:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl describe deployment/config-server-deployment -n default || true"
                    echo "Config Server 파드 로그 확인 (왜 DOWN 상태인지 확인):"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n default -l app=config-server -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- Config Server 파드 {} 로그: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n default || true; echo \"\";' || true"
                    echo "--- Config Server 상태 상세 디버깅 끝 ---"
                    // --- Config Server 자체 디버깅 끝 ---

                    retry(3) {
                        sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl wait --for=condition=available deployment/eureka-server-deployment -n default --timeout=600s || exit 1"
                    }
                    echo "Eureka Server is ready. Proceeding with user-admin-repo bundle."

                    def ecrRepoName = 'msa-user-admin-repo'
                    def fullEcrRepoUrl = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/${ecrRepoName}"

                    echo "--- Building and Deploying user-admin-repo bundle ---"
                    def dockerImage = docker.build("${fullEcrRepoUrl}:${env.IMAGE_TAG}", "${params.FORCE_FULL_DEPLOY ? '--no-cache' : ''} .")

                    // ECR 로그인 및 이미지 푸시
                    withCredentials([aws(credentialsId: 'aws-cicd-credentials')]) {
                        sh "aws ecr get-login-password --region ${env.AWS_REGION} | docker login --username AWS --password-stdin ${fullEcrRepoUrl.split('/')[0]}"
                        dockerImage.push("${env.IMAGE_TAG}")
                        dockerImage.push("latest") // latest 태그도 함께 푸시
                    }

                    // --- 기존 배포 강제 삭제 (새로운 이미지의 클린한 롤아웃을 보장하기 위해) ---
                    echo "--- 기존 배포 강제 삭제 (새로운 이미지 클린 롤아웃 보장) ---"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl delete deployment user-admin-repo-deployment -n default --ignore-not-found || true"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl wait --for=delete deployment/user-admin-repo-deployment -n default --timeout=300s --for=delete || true"
                    echo "--- 기존 배포 삭제 완료 (존재했다면) ---"
                    // --- 기존 배포 강제 삭제 끝 ---

                    echo "--- Creating/Updating OAuth Secrets ---"
                    sh '''
                    KUBECONFIG=${KUBECONFIG_PATH} kubectl apply -f - <<EOF
                    apiVersion: v1
                    kind: Secret
                    metadata:
                      name: oauth-secrets
                      namespace: default
                    type: Opaque
                    stringData:
                      NAVER_CLIENT_ID: "${OAUTH_NAVER_CLIENT_ID}"
                      NAVER_CLIENT_SECRET: "${OAUTH_NAVER_CLIENT_SECRET}"
                      KAKAO_CLIENT_ID: "${OAUTH_KAKAO_CLIENT_ID}"
                      GOOGLE_CLIENT_ID: "${OAUTH_GOOGLE_CLIENT_ID}"
                      GOOGLE_CLIENT_SECRET: "${OAUTH_GOOGLE_CLIENT_SECRET}"
                    EOF
                    '''
                    echo "--- OAuth Secrets are set ---"

                    sh """
                        KUBECONFIG=${env.KUBECONFIG_PATH} sed -i "s|__ECR_IMAGE_FULL_PATH__|${fullEcrRepoUrl}:${env.IMAGE_TAG}|g" k8s/deployment.yaml
                        KUBECONFIG=${env.KUBECONFIG_PATH} kubectl apply -f k8s/deployment.yaml -n default
                        KUBECONFIG=${env.KUBECONFIG_PATH} kubectl apply -f k8s/service.yaml -n default
                    """

                    // --- Kubernetes Deployment Debugging (user-admin-repo 파드 관련) ---
                    echo "--- Kubernetes Deployment Debugging (user-admin-repo 파드 관련) ---"
                    echo "배포 상태 확인 전 파드 목록:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n default -l app=user-admin-repo || true"
                    echo "배포 이벤트 확인:"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl describe deployment/user-admin-repo-deployment -n default || true"

                    echo "파드 로그 확인 (메인 컨테이너):"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n default -l app=user-admin-repo -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- 메인 컨테이너 {} 로그: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n default -c user-admin-repo-container || true; echo \"\";' || true"

                    echo "파드 초기화 컨테이너 로그 확인 (wait-for-config-server):"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n default -l app=user-admin-repo -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- 초기화 컨테이너 (config-server) {} 로그: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n default -c wait-for-config-server || true; echo \"\";' || true"

                    echo "파드 초기화 컨테이너 로그 확인 (wait-for-eureka):"
                    sh "KUBECONFIG=${env.KUBECONFIG_PATH} kubectl get pods -n default -l app=user-admin-repo -o custom-columns=NAME:.metadata.name --no-headers | xargs -r -I {} sh -c 'echo \"--- 초기화 컨테이너 (eureka) {} 로그: ---\"; KUBECONFIG=${env.KUBECONFIG_PATH} kubectl logs {} -n default -c wait-for-eureka || true; echo \"\";' || true"

                    echo "--- End Kubernetes Deployment Debugging (user-admin-repo 파드 관련) ---"
                    // --- 디버깅 끝 ---

                    sh """
                        KUBECONFIG=${env.KUBECONFIG_PATH} kubectl rollout status deployment/user-admin-repo-deployment -n default --timeout=600s || exit 1
                    """
                    echo "User-Admin-Repo bundle 배포 완료."
                }
            }
        }
    }

    post {
        always {
            cleanWs() // 워크스페이스 정리
        }
        failure {
            echo "CI/CD Pipeline for user-admin-repo failed."
        }
        success {
            echo "CI/CD Pipeline for user-admin-repo finished successfully."
        }
    }
}