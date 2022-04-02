package org.jenkinsci.plugins.gitea_pat_kubernetes_credentials;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.jenkinsci.plugin.gitea.credentials.PersonalAccessTokenImpl;
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
            PersonalAccessTokenImpl credential = convertor.convert(secret);
            assertThat(credential).isNotNull();
            assertThat(credential.getId()).isEqualTo("a-test-giteapat");
            assertThat(credential.getDescription()).isEqualTo("credentials from Kubernetes");
            assertThat(credential.getToken()).isEqualTo(hudson.util.Secret.fromString("0123456789012345678901234567890123456789"));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws CredentialsConvertionException, IOException {
        GiteaPATCredentialsConvertor convertor = new GiteaPATCredentialsConvertor();
        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat(secret).isNotNull();
            PersonalAccessTokenImpl credential = convertor.convert(secret);
            assertThat(credential).isNotNull();
            assertThat(credential.getId()).isEqualTo("a-test-giteapat");
            assertThat(credential.getDescription()).isNullOrEmpty();
            assertThat(credential.getToken()).isEqualTo(hudson.util.Secret.fromString("0123456789012345678901234567890123456789"));
        }
    }

    @Test
    public void failsToConvertWhenTokenMissing() throws Exception {
        GiteaPATCredentialsConvertor convertor = new GiteaPATCredentialsConvertor();
        try (InputStream is = get("missing-token.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThatThrownBy(() -> convertor.convert(secret))
                    .isInstanceOf(CredentialsConvertionException.class)
                    .hasMessage("gitea credential is missing token (mapped to token)");

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
