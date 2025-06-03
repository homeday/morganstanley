curl -L -o kubeconform https://github.com/yannh/kubeconform/releases/latest/download/kubeconform-linux-amd64
chmod +x kubeconform
sudo mv kubeconform /usr/local/bin/


Basic check:
kubeconform my-manifest.yaml
Validate all YAMLs in a directory:

Validate all YAMLs in a directory:
kubeconform ./manifests/

Validate rendered Helm chart:
helm template my-release ./chart | kubeconform -

Use strict mode + summary:
kubeconform -strict -summary ./manifests/


flux validate helmrelease -f ./helmrelease.yaml

flux validate kustomization -f ./path/to/kustomization.yaml

# Validate Kustomization with kubeconform
kustomize build ./path/to/kustomization | kubeconform -


https://github.com/jenkinsci/git-plugin/blob/9669e4588be4aedbc208cd83c381e6cd3f7d55c1/src/main/java/hudson/plugins/git/UserRemoteConfig.java#L176

https://github.com/jenkinsci/jenkins/blob/4f6921b8288ea50aa4e605fcd34a08deb7626014/core/src/main/java/hudson/model/StringParameterValue.java
https://github.com/jenkinsci/jenkins/blob/4f6921b8288ea50aa4e605fcd34a08deb7626014/core/src/main/java/hudson/model/StringParameterDefinition.java#L53

https://www.jenkins.io/doc/developer/security/form-validation/

https://www.jenkins.io/doc/developer/forms/form-validation/