package org.jenkinsci.plugins.github_app_kubernetes_credentials;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;


public class GithubAppCredentialsConvertorTest {

    @Test
    public void canConvert() {
        GithubAppCredentialsConvertor convertor = new GithubAppCredentialsConvertor();
        assertThat(convertor.canConvert("githubApp")).isTrue();
        assertThat(convertor.canConvert("something")).isFalse();
    }

    @Test
    public void canConvertAValidSecret() throws CredentialsConvertionException, IOException {
        GithubAppCredentialsConvertor convertor = new GithubAppCredentialsConvertor();
        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat(secret).isNotNull();
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential).isNotNull();
            assertThat(credential.getId()).isEqualTo("a-test-githubapp");
            assertThat(credential.getDescription()).isEqualTo("credentials from Kubernetes");
            assertThat(credential.getAppID()).isEqualTo("12");
            assertThat(credential.getPrivateKey().getPlainText()).isEqualTo("some private key content");
            assertThat(credential.getApiUri()).isEqualTo("https://host.github/api/v3");
            assertThat(credential.getOwner()).isEqualTo("owner1");

        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws CredentialsConvertionException, IOException {
        GithubAppCredentialsConvertor convertor = new GithubAppCredentialsConvertor();
        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat(secret).isNotNull();
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential).isNotNull();
            assertThat(credential.getId()).isEqualTo("a-test-githubapp");
            assertThat(credential.getDescription()).isNullOrEmpty();
            assertThat(credential.getAppID()).isEqualTo("12");
            assertThat(credential.getPrivateKey().getPlainText()).isEqualTo("some private key content");
            assertThat(credential.getApiUri()).isEqualTo("https://host.github/api/v3");
            assertThat(credential.getOwner()).isEqualTo("owner1");

        }
    }

    @Test
    public void failsToConvertWhenAppIdMissing() throws Exception {
        GithubAppCredentialsConvertor convertor = new GithubAppCredentialsConvertor();
        try (InputStream is = get("missing-app-id.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThatThrownBy(() -> convertor.convert(secret))
                    .isInstanceOf(CredentialsConvertionException.class)
                    .hasMessage("github app credential is missing appId");

        }
    }

    @Test
    public void failsToConvertWhenPrivateKeyMissing() throws Exception {
        GithubAppCredentialsConvertor convertor = new GithubAppCredentialsConvertor();
        try (InputStream is = get("missing-private-key.yaml")) {

            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThatThrownBy(() -> convertor.convert(secret))
                    .isInstanceOf(CredentialsConvertionException.class)
                    .hasMessage("github app credential is missing privateKey");

        }
    }


    private static InputStream get(String resource) {
        InputStream is = GithubAppCredentialsConvertor.class.getResourceAsStream("GithubAppCredentialsConvertor/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
