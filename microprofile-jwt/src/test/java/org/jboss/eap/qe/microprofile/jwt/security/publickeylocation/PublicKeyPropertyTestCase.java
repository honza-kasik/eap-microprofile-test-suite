package org.jboss.eap.qe.microprofile.jwt.security.publickeylocation;

import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.cdi.BasicCdiTest;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsBasicEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Set of tests verifying functionality of {@code mp.jwt.verify.publickey} property.
 */
@RunWith(Arquillian.class)
public class PublicKeyPropertyTestCase {

    private static final String DEPLOYMENT_WITH_VALID_KEY = "valid-key-deployment";
    private static final String DEPLOYMENT_WITH_INVALID_KEY = "invalid-key-deployment";

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = BasicCdiTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    /**
     * A deployment with valid key. This means, that it is correct key which can be used to verify the JWT signature.
     * @return a deployment
     */
    @Deployment(name = DEPLOYMENT_WITH_VALID_KEY)
    public static WebArchive createValidKeyDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(JaxRsBasicEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(BasicCdiTest.class.getClassLoader().getResource("mp-config-pk-valid.properties"),
                        "microprofile-config.properties");
    }

    /**
     * A deployment with invalid key. The verification fails when this key is used.
     * @return a deployment
     */
    @Deployment(name = DEPLOYMENT_WITH_INVALID_KEY)
    public static WebArchive createInvalidKeyDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(JaxRsBasicEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(BasicCdiTest.class.getClassLoader().getResource("mp-config-pk-invalid.properties"),
                        "microprofile-config.properties");
    }

    /**
     * @tpTestDetails A request with proper JWT is sent on server which has configured bad public key. The server fails
     * to verify the JWT signature and won't authorize the client. Test specification compatibility.
     * @tpPassCrit "Unauthorized" message is shown to user and warning is present in log specifying the cause of fail.
     * @tpSince EAP 7.3.0.CD19
     */
    @Test
    @RunAsClient
    @OperateOnDeployment(DEPLOYMENT_WITH_INVALID_KEY)
    public void testJwkVerificationFailsWithInvalidKey(@ArquillianResource URL url) throws InterruptedException {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        RestAssured.given()
                .header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + "basic-endpoint")
                .then()
                .body(equalTo("<html><head><title>Error</title></head><body>Unauthorized</body></html>"));

        //TODO add check for expected log message with correct reason
    }

    /**
     * @tpTestDetails A negative scenario where request with proper JWT is sent on server which has configured bad
     * public key. The server verifies the signature and authorizes user.
     * @tpPassCrit Token which was sent on server is sent back to client in response.
     * @tpSince EAP 7.3.0.CD19
     */
    @Test
    @RunAsClient
    @OperateOnDeployment(DEPLOYMENT_WITH_VALID_KEY)
    public void testJwkVerificationSucceedsWithValidKey(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        RestAssured.given()
                .header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + "basic-endpoint")
                .then().body(equalTo(token.getRawValue()));

    }
}
