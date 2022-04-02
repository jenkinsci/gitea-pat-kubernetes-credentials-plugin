package org.jenkinsci.plugins.gitea_app_kubernetes_credentials;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugin.gitea.credentials;
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

@OptionalExtension(requirePlugins = {"gitea", "kubernetes-credentials-provider"})
public class GiteaAppCredentialsConvertor extends SecretToCredentialConverter {
    private static final Logger LOG = Logger.getLogger(GiteaAppCredentialsConvertor.class.getName());

    @Override
    public boolean canConvert(String type) {
        return "giteaApp".equals(type);
    }

    @Override
    public GiteaAppCredentials convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "gitea app credential definition contains no data");
        String appIdBase64 = SecretUtils.getNonNullSecretData(secret, "appId", "gitea app credential is missing appId");
        String appId = decodeBase64(appIdBase64, "Not a valid appId");
        hudson.util.Secret privateKeySecret = hudson.util.Secret.fromString(privateKey);
        GiteaAppCredentials giteaAppCredentials = new GiteaAppCredentials(
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
                "gitea app credential : failed to retrieve optional parameter apiUri");
        if (apiUrlOptional.isPresent()) {
            String apiUrl = decodeBase64(apiUrlOptional.get(), "Not a valid apiUri");
            giteaAppCredentials.setApiUri(apiUrl);
        }
        Optional<String> ownerOptional = SecretUtils.getOptionalSecretData(secret,
                "owner",
                "gitea app credential : failed to retrieve optional parameter owner");
        if (ownerOptional.isPresent()) {
            String owner = decodeBase64(ownerOptional.get(), "Not a valid apiUri");
            giteaAppCredentials.setOwner(owner);
        }
        return giteaAppCredentials;

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
