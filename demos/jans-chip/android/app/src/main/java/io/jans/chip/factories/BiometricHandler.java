package io.jans.chip.factories;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;
import java.util.concurrent.Executor;

import io.jans.chip.FidoEnrolmentActivity;
import io.jans.chip.LoginActivity;
import io.jans.chip.authenticator.AuthenticatorAdaptor;
import io.jans.chip.modal.Fido.config.FidoConfigurationResponse;
import io.jans.chip.modal.UserInfoResponse;
import io.jans.chip.utils.AppConfig;

public class BiometricHandler {
    private String signedMessage;
    private String operationType;
    private Context ctx;
    Map<String, String> bundle;

    public static final String TAG = BiometricHandler.class.getName();

    public BiometricHandler(Context ctx) {
        this.ctx = ctx;
    }

    public BiometricHandler(Context ctx, Map<String, String> bundle) {
        this.bundle = bundle;
        this.ctx = ctx;
    }

    public String getSignedMessage() {
        return signedMessage;
    }

    public void setSignedMessage(String signedMessage) {
        this.signedMessage = signedMessage;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public boolean canAuthenticateWithStrongBiometrics() {
        return BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    @Nullable
    public Signature initSignature(String keyName) throws Exception {
        KeyPair keyPair = getKeyPair(keyName);

        if (keyPair != null) {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            return signature;
        }
        return null;
    }

    @Nullable
    public KeyPair getKeyPair(String keyName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(keyName)) {
            // Get public key
            PublicKey publicKey = keyStore.getCertificate(keyName).getPublicKey();
            // Get private key
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyName, null);
            // Return a key pair
            return new KeyPair(publicKey, privateKey);
        }
        return null;
    }
    public BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        // Callback for biometric authentication result
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Log.e(TAG, "Error code: " + errorCode + "error String: " + errString);
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                Log.i(TAG, "onAuthenticationSucceeded");
                super.onAuthenticationSucceeded(result);
                if (result.getCryptoObject() != null && result.getCryptoObject().getSignature() != null) {
                    try {
                        Signature signature = result.getCryptoObject().getSignature();
                        signature.update(signedMessage.getBytes());
                        String signatureString = Base64.encodeToString(signature.sign(), Base64.URL_SAFE);
                        // Normally, ToBeSignedMessage and Signature are sent to the server and then verified
                        Log.i(TAG, "Message: " + signedMessage);
                        Log.i(TAG, "Signature (Base64 Encoded): " + signatureString);
                        //Toast.makeText(ctx, signedMessage + ":" + signatureString, Toast.LENGTH_SHORT).show();

                        //attestationOption();
                        if (operationType.equals(AppConfig.FIDO_ENROLMENT)) {
                            AuthenticatorAdaptor authenticatorAdaptor = AuthenticatorAdaptor.newInstance(ctx);
                            authenticatorAdaptor.attestationOption();
                        } else if (operationType.equals(AppConfig.FIDO_AUTHENTICATION)) {
                            AuthenticatorAdaptor authenticatorAdaptor = AuthenticatorAdaptor.newInstance(ctx, bundle);
                            authenticatorAdaptor.assertionOption();
                        }


                    } catch (SignatureException e) {
                        throw new RuntimeException();
                    }
                } else {
                    // Error
                    Toast.makeText(ctx, "Something wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        };
    }

    public Executor getMainThreadExecutor() {
        return new BiometricHandler.MainThreadExecutor();
    }



    public static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable r) {
            handler.post(r);
        }
    }

    public KeyPair generateKeyPair(String keyName, boolean invalidatedByBiometricEnrollment) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA384,
                        KeyProperties.DIGEST_SHA512)
                // Require the user to authenticate with a biometric to authorize every use of the key
                .setUserAuthenticationRequired(true);

        // Generated keys will be invalidated if the biometric templates are added more to user device
        if (Build.VERSION.SDK_INT >= 24) {
            builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
        }

        keyPairGenerator.initialize(builder.build());

        return keyPairGenerator.generateKeyPair();
    }

}
