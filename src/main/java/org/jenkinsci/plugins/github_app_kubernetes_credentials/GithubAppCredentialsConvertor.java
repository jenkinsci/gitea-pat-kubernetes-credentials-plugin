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
        SecretUtils.getOptionalSecretData(
                secret
                , "apiUri"
                , "github app credential : failed to retrieve optional parameter apiUri")
                .map(apiUriBase64 -> decodeBase64(apiUriBase64, "Not a valid apiUri"))
                .ifPresent(
                        gitHubAppCredentials::setApiUri
                );
        SecretUtils.getOptionalSecretData(secret
                , "owner"
                , "github app credential : failed to retrieve optional parameter owner")
                .map(ownerBase64 -> decodeBase64(ownerBase64, "Not a valid owner"))
                .ifPresent(
                        gitHubAppCredentials::setOwner
                );
        return gitHubAppCredentials;

    }

    private String decodeBase64(String base64, String s) {
        try {
            return SecretUtils.requireNonNull(base64DecodeToString(base64), s);
        } catch (CredentialsConvertionException e) {
            throw new RuntimeException(e);
        }
    }

    static String base64DecodeToString(String s) {
        byte[] bytes = base64Decode(s);
        if (bytes != null) {
            try {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                decoder.onMalformedInput(CodingErrorAction.REPORT);
                decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer decode = decoder.decode(ByteBuffer.wrap(bytes));
                return decode.toString();
            } catch (CharacterCodingException ex) {
                LOG.log(Level.WARNING, "failed to covert Secret, is this a valid UTF-8 string?  {0}", ex.getMessage());
            }
        }
        return null;
    }


    static byte[] base64Decode(String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "failed to base64decode Secret, is the format valid?  {0}", ex.getMessage());
        }
        return null;
    }
}
