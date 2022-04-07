package io.jenkins.plugins.gitea_pat_kubernetes_credentials;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugin.gitea.credentials.PersonalAccessTokenImpl;
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
public class GiteaPATCredentialsConvertor extends SecretToCredentialConverter {
    private static final Logger LOG = Logger.getLogger(GiteaPATCredentialsConvertor.class.getName());

    @Override
    public boolean canConvert(String type) {
        return "giteaPAT".equals(type);
    }

    @Override
    public PersonalAccessTokenImpl convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "gitea credential definition contains no data");
        String tokenBase64 = SecretUtils.getNonNullSecretData(secret, "token", "gitea credential is missing token");
        String token = decodeBase64(tokenBase64, "Not a valid token");
        PersonalAccessTokenImpl giteaPATCredentials = new PersonalAccessTokenImpl(
                // Scope
                CredentialsScope.GLOBAL,
                // ID
                SecretUtils.getCredentialId(secret),
                // Description
                SecretUtils.getCredentialDescription(secret),
                // token
                token
        );
        return giteaPATCredentials;

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
