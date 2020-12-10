package org.jenkinsci.plugins.github_app_kubernetes_credentials;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;
import org.jenkinsci.plugins.variant.OptionalExtension;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@OptionalExtension(requirePlugins = {"github-branch-source", "kubernetes-credentials-provider"})
public class GithubAppCredentialsConvertor extends SecretToCredentialConverter {
    private static final Logger LOG = Logger.getLogger(GithubAppCredentialsConvertor.class.getName());

    @Override
    public boolean canConvert(String type) {
        return "githubApp".equals(type);
    }

    @Override
    public GitHubAppCredentials convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "github app credential definition contains no data");
        String appIdBase64 = SecretUtils.getNonNullSecretData(secret, "appId", "github app credential is missing appId");
        String privateKeyBase64 = SecretUtils.getNonNullSecretData(secret, "privateKey", "github app credential is missing privateKey");
        String appId = decodeBase64(appIdBase64, "Not a valid appId");
        String privateKey = decodeBase64(privateKeyBase64, "Not a valid privateKey");
        hudson.util.Secret privateKeySecret = hudson.util.Secret.fromString(privateKey);
        GitHubAppCredentials gitHubAppCredentials = new GitHubAppCredentials(
                // Scope
                CredentialsScope.GLOBAL,
                // ID
                SecretUtils.getCredentialId(secret),
                // Description
                SecretUtils.getCredentialDescription(secret),
                // appId
                appId,
                // privateKey
                privateKeySecret
        );
        Optional<String> apiUrlOptional = SecretUtils.getOptionalSecretData(
                secret,
                "apiUri",
                "github app credential : failed to retrieve optional parameter apiUri");
        if (apiUrlOptional.isPresent()) {
            String apiUrl = decodeBase64(apiUrlOptional.get(), "Not a valid apiUri");
            gitHubAppCredentials.setApiUri(apiUrl);
        }
        Optional<String> ownerOptional = SecretUtils.getOptionalSecretData(secret,
                "owner",
                "github app credential : failed to retrieve optional parameter owner");
        if (ownerOptional.isPresent()) {
            String owner = decodeBase64(ownerOptional.get(), "Not a valid apiUri");
            gitHubAppCredentials.setOwner(owner);
        }
        return gitHubAppCredentials;

    }

    private String decodeBase64(String base64, String errorMessage) throws CredentialsConvertionException {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            if (bytes != null) {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                decoder.onMalformedInput(CodingErrorAction.REPORT);
                decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer decode = decoder.decode(ByteBuffer.wrap(bytes));
                return SecretUtils.requireNonNull(decode.toString(), errorMessage);
            } else {
                throw new CredentialsConvertionException(errorMessage);
            }

        } catch (CharacterCodingException e) {
            LOG.log(Level.WARNING, "failed to decode Secret, is the format valid? {0} {1}", new String[]{base64, e.getMessage()});
            throw new CredentialsConvertionException(errorMessage, e);
        }
    }


}
