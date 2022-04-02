package org.jenkinsci.plugins.gitea_pat_kubernetes_credentials;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.jenkinsci.plugin.gitea.credentials.PersonalAccessTokenImpl;
import org.jenkinsci.plugin.gitea.credentials.PersonalAccessToken;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;


public class GiteaPATCredentialsConvertorTest {

    @Test
    public void canConvert() {
        GiteaPATCredentialsConvertor convertor = new GiteaPATCredentialsConvertor();
        assertThat(convertor.canConvert("giteaPAT")).isTrue();
        assertThat(convertor.canConvert("something")).isFalse();
    }

    @Test
    public void canConvertAValidSecret() throws CredentialsConvertionException, IOException {
        GiteaPATCredentialsConvertor convertor = new GiteaPATCredentialsConvertor();
        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat(secret).isNotNull();
            PersonalAccessToken credential = convertor.convert(secret);
            assertThat(credential).isNotNull();
            assertThat(credential.getId()).isEqualTo("a-test-giteapat");
            assertThat(credential.getDescription()).isEqualTo("credentials from Kubernetes");
            assertThat(credential.getToken()).isEqualTo("0123456789012345678901234567890123456789");
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws CredentialsConvertionException, IOException {
        GiteaPATCredentialsConvertor convertor = new GiteaPATCredentialsConvertor();
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
        GiteaPATCredentialsConvertor convertor = new GiteaPATCredentialsConvertor();
        try (InputStream is = get("missing-app-id.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThatThrownBy(() -> convertor.convert(secret))
                    .isInstanceOf(CredentialsConvertionException.class)
                    .hasMessage("github app credential is missing appId");

        }
    }

    private static InputStream get(String resource) {
        InputStream is = GiteaPATCredentialsConvertor.class.getResourceAsStream("GiteaPATCredentialsConvertor/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
