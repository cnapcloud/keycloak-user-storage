pipeline {
    agent { label 'build' }

	environment {
       GITHUB_TOKEN=credentials('github-token')
       HARBOR_PASSWD=credentials('harbor-password')
    }

	options {
		disableConcurrentBuilds()
		timeout(time: 30, unit: 'MINUTES') 
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage('setup-github') {
			steps {
                sh '''
                   ### git user info !! important to reuse pod. 
                   git config --global user.email "mirinus@gmail.com"
                   git config --global user.name "lemon"
				   git config --global url.https://mirinus:${GITHUB_TOKEN}@github.com/cnapcloud/.insteadOf https://github.com/cnapcloud/
				'''
			}	
        }
		
		stage("build") {
			steps {
			   sh '''
                  make build
                '''        			       			
		    }
	    }
	    
	   stage('push image') {	
	        steps {
                sh '''
                     docker login harbor.cnapcloud.com -u admin -p ${HARBOR_PASSWD}
                     make docker-build
			    ''' 	 
	  	    }
	    }

        stage('update gitops') {
            steps {
                sh '''
                    make update-tag
                 '''         
            }
        }
	    

	}	

}	
