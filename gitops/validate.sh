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
