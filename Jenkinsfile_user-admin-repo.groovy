pipeline {
    agent any

    // Jenkins Job 실행 시 수동으로 전체 재배포를 강제할 수 있는 파라미터
    parameters {
        booleanParam(defaultValue: true, description: 'Force full deployment of user-admin-repo (ignores changes).', name: 'FORCE_FULL_DEPLOY')
    }

    environment {
        // AWS 계정 및 리전 정보
        AWS_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID = '947625948810' // 실제 AWS 계정 ID로 변경 필수

        // EKS 클러스터 정보
        EKS_CLUSTER_NAME = 'msa-eks-cluster' // 실제 EKS 클러스터 이름으로 변경 필수
        EKS_KUBECTL_ROLE_ARN = "arn:aws:iam::${AWS_ACCOUNT_ID}:role/JenkinsEKSDeployerRole" // 실제 IAM Role ARN으로 변경 필수

        // Docker 이미지 태그 (Jenkins 빌드 번호 사용)
        IMAGE_TAG = "${env.BUILD_NUMBER}"

        // kubectl 설정 파일 경로
        KUBECONFIG_PATH = "${WORKSPACE}/kubeconfig"
    }

    // GitHub 웹훅 트리거 (deployAWS 브랜치에 푸시 시)
    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                // deployAWS 브랜치 체크아웃
                // credentialsId는 Jenkins에 등록된 GitHub PAT Credential ID여야 합니다.
                // 이 레포지토리의 크리덴셜 ID를 사용하세요 (예: 'github-user-admin-repo-pat')
                git branch: 'deployAWS', credentialsId: 'github-user-admin-repo-pat', url: 'https://github.com/jigubangbang/user-admin-repo.git'
            }
        }

        stage('Set AWS Kubeconfig') {
            steps {
                script {
                    // EKS 클러스터 접근을 위한 kubeconfig 설정
                    // 'aws-cicd-credentials'는 Jenkins에 등록된 AWS IAM 사용자/역할 Credential ID여야 합니다.
                    withCredentials([aws(credentialsId: 'aws-cicd-credentials')]) {
                        sh "aws eks update-kubeconfig --name ${env.EKS_CLUSTER_NAME} --region ${env.AWS_REGION} --kubeconfig ${env.KUBECONFIG_PATH} --role-arn ${env.EKS_KUBECTL_ROLE_ARN}"
                    }
                }
            }
        }

        stage('Deploy User-Admin-Repo Bundle') {
            steps {
                script {
                    // 번들 서비스는 Config Server와 Eureka Server가 준비될 때까지 기다림
                    // 이전에 infra-platform Jenkinsfile에서 배포했으므로 여기서는 대기만 합니다.
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

                    def ecrRepoName = 'msa-user-admin-repo' // ECR 레포지토리 이름 (번들용)
                    def fullEcrRepoUrl = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/${ecrRepoName}"

                    echo "--- Building and Deploying user-admin-repo bundle ---"
                    // user-admin-repo의 루트 디렉토리에서 Dockerfile이 존재하므로 dir() 필요 없음
                    // Docker 이미지 빌드 (FORCE_FULL_DEPLOY 시 --no-cache)
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

                    // Kubernetes Deployment/Service YAML 업데이트 및 적용
                    // k8s YAML 파일들은 user-admin-repo/k8s/ 디렉토리에 있다고 가정
                    sh """
                        KUBECONFIG=${env.KUBECONFIG_PATH} sed -i "s|__ECR_IMAGE_FULL_PATH__|${fullEcrRepoUrl}:${env.IMAGE_TAG}|g" k8s/deployment.yaml
                        KUBECONFIG=${env.KUBECONFIG_PATH} kubectl apply -f k8s/deployment.yaml -n default
                        KUBECONFIG=${env.KUBECONFIG_PATH} kubectl apply -f k8s/service.yaml -n default
                    """

                    // --- Kubernetes Deployment Debugging (user-admin-repo 파드 관련) ---
                    echo "--- Kubernetes Deployment Debugging (user-admin-repo 파드 관련) ---"
                    echo "배포 상태 확인 전 파드 목록:"
                    // 'app' 레이블은 k8s/deployment.yaml의 spec.selector.matchLabels에 있는 값을 사용해야 합니다.
                    // 예시: app=user-admin-repo
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