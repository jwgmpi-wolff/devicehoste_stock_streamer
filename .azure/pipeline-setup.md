# Azure pipeline setup

The workflow uses Microsoft Entra workload identity federation. It does not use a client secret, storage key, or connection string.

1. Create a resource group for a user-assigned managed identity.
2. Create the identity and federated credential whose subject is `repo:jerrywolff_microsoft/devicehoste_stock_streamer:environment:production`, issuer is `https://token.actions.githubusercontent.com`, and audience is `api://AzureADTokenExchange`.
3. Grant the identity `Contributor` on the target deployment resource group and `Storage Blob Data Contributor` on the subscription or target storage account. Narrow the latter scope to the storage account after its first deployment.
4. Create a protected GitHub environment named `production` and add approval rules.
5. Add these GitHub environment variables: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, and `AZURE_LOCATION`.
6. Run **Build and deploy Android distribution** from the repository Actions tab.

The infrastructure template creates HTTPS-only Azure Storage and disables shared-key authorization. The authenticated workflow enables static website hosting through the supported data-plane API. The published APK has no embedded market-provider key.