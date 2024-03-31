package io.jans.chip.authenticator;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import duo.labs.webauthn.exceptions.VirgilException;
import duo.labs.webauthn.exceptions.WebAuthnException;
import duo.labs.webauthn.models.AttestationObject;
import duo.labs.webauthn.models.AuthenticatorGetAssertionOptions;
import duo.labs.webauthn.models.AuthenticatorGetAssertionResult;
import duo.labs.webauthn.models.AuthenticatorMakeCredentialOptions;
import duo.labs.webauthn.models.PublicKeyCredentialDescriptor;
import duo.labs.webauthn.models.PublicKeyCredentialSource;
import duo.labs.webauthn.models.RpEntity;
import duo.labs.webauthn.models.UserEntity;
import duo.labs.webauthn.util.CredentialSelector;
import io.jans.chip.AfterLoginActivity;
import io.jans.chip.AppDatabase;
import io.jans.chip.LoginActivity;
import io.jans.chip.R;
import io.jans.chip.modal.Fido.assertion.option.AssertionOptionRequest;
import io.jans.chip.modal.Fido.assertion.option.AssertionOptionResponse;
import io.jans.chip.modal.Fido.assertion.result.AssertionResultRequest;
import io.jans.chip.modal.Fido.attestation.option.AttestationOptionRequest;
import io.jans.chip.modal.Fido.attestation.option.AttestationOptionResponse;
import io.jans.chip.modal.Fido.attestation.result.AttestationResultRequest;
import io.jans.chip.modal.Fido.config.FidoConfiguration;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.repository.UserInfoResponseRepository;
import io.jans.chip.retrofit.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthenticatorAdaptor {
    private static final String TAG = AuthenticatorAdaptor.class.getName();
    Authenticator authenticator;
    public static final String USER_INFO = "io.jans.DPoP.LoginActivity.USER_INFO";
    Context ctx;
    AppDatabase appDatabase;
    AlertDialog.Builder errorDialog;
    List<FidoConfiguration> fidoConfigurationList;
    List<OIDCClient> oidcClientList;
    Map<String, String> bundle;

    public AuthenticatorAdaptor(Context ctx, Map<String, String> bundle) {
        // Required empty public constructor
        try {
            this.ctx = ctx;
            this.bundle = bundle;
            appDatabase = AppDatabase.getInstance(ctx);
            fidoConfigurationList = appDatabase.fidoConfigurationDao().getAll();
            oidcClientList = appDatabase.oidcClientDao().getAll();
            errorDialog = new AlertDialog.Builder(ctx);
            authenticator = new Authenticator(ctx, false, false);
        } catch (VirgilException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AuthenticatorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AuthenticatorAdaptor newInstance(Context ctx , Map<String, String> bundle) {
        AuthenticatorAdaptor adaptor = new AuthenticatorAdaptor(ctx, bundle);
        return adaptor;
    }
    public static AuthenticatorAdaptor newInstance(Context ctx) {
        AuthenticatorAdaptor adaptor = new AuthenticatorAdaptor(ctx, null);
        return adaptor;
    }


    public void attestationOption() {
        Log.d(TAG, "-------------------------------------------------------------------------");
        Log.d(TAG, "attestationOption :: ");
        AttestationOptionRequest req = new AttestationOptionRequest();
        req.setAttestation("none");
        req.setUsername("admin");
        req.setDisplayName("admin");

        List<FidoConfiguration> fidoConfigurationList = appDatabase.fidoConfigurationDao().getAll();
        if (fidoConfigurationList == null || fidoConfigurationList.isEmpty()) {
            createErrorDialog("Fido configuration not found in database.");
            errorDialog.show();
            //loginButton.setEnabled(false);
            return;
        }
        FidoConfiguration fidoConfiguration = fidoConfigurationList.get(0);
        Call<AttestationOptionResponse> call = RetrofitClient.getInstance(fidoConfiguration.getIssuer())
                .getAPIInterface().attestationOption(req, fidoConfiguration.getAttestationOptionsEndpoint());

        call.enqueue(new Callback<AttestationOptionResponse>() {
            @Override
            public void onResponse(Call<AttestationOptionResponse> call, Response<AttestationOptionResponse> response) {

                AttestationOptionResponse responseFromAPI = response.body();
                Log.d(TAG, "+++++++++++++++++++++++++++++++++++++++++++++" + responseFromAPI.toString());
                if (response.code() == 200 || response.code() == 201) {
                    if (responseFromAPI.getChallenge() != null) {
                        try {
                            Log.d(TAG, "+++++++++++++++++++++++++++++++++++++++++++++" + responseFromAPI.toString());
                            register(responseFromAPI);
                        } catch (VirgilException e) {
                            throw new RuntimeException(e);
                        } catch (WebAuthnException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    //return null;
                }
            }

            @Override
            public void onFailure(Call<AttestationOptionResponse> call, Throwable t) {
                Log.e(TAG, "Inside doDCR :: onFailure :: " + t.getMessage());

            }
        });
    }

    public void register(AttestationOptionResponse responseFromAPI) throws VirgilException, WebAuthnException, NoSuchAlgorithmException, JsonProcessingException {
        AuthenticatorMakeCredentialOptions options = new AuthenticatorMakeCredentialOptions();
        options.rpEntity = new RpEntity();
        options.rpEntity.id = responseFromAPI.getRp().getId();
        options.rpEntity.name = responseFromAPI.getRp().getName();

        options.userEntity = new UserEntity();
        options.userEntity.id = responseFromAPI.getUser().getId().getBytes();//"vScQ9Aec2Z8RKNvfZhpg375RWVIN1QMf8x_q9houJnc".getBytes();
        options.userEntity.name = responseFromAPI.getUser().getName();//"admin";
        options.userEntity.displayName = responseFromAPI.getUser().getDisplayName();//"admin";
        options.clientDataHash = generateClientDataHash(responseFromAPI.getChallenge(), "webauthn.create", "https://admin-ui-test.gluu.org");

        options.requireResidentKey = false;
        options.requireUserPresence = true;
        options.requireUserVerification = false;
        options.excludeCredentialDescriptorList = Lists.newArrayList();

        List<Pair<String, Long>> credTypesAndPubKeyAlgs = new ArrayList<>();
        Pair<String, Long> pair = new Pair<>("public-key", -7L);
        credTypesAndPubKeyAlgs.add(pair);
        options.credTypesAndPubKeyAlgs = credTypesAndPubKeyAlgs;
        AttestationObject attestationObject = authenticator.makeCredential(options);
        byte[] attestationObjectBytes = attestationObject.asCBOR();
        Log.d(TAG + "attestationObjectBytes :", urlEncodeToString(attestationObjectBytes));
        Log.d(TAG, urlEncodeToString(attestationObject.getCredentialId()).replace("\n", ""));
        Log.d(TAG, urlEncodeToString(attestationObject.getCredentialId()));
        AttestationResultRequest attestationResultRequest = new AttestationResultRequest();
        attestationResultRequest.setId(urlEncodeToString(attestationObject.getCredentialId()).replace("\n", "").replace("=", ""));

        attestationResultRequest.setType("public-key");

        io.jans.chip.modal.Fido.attestation.result.Response response = new io.jans.chip.modal.Fido.attestation.result.Response();
        response.setAttestationObject(urlEncodeToString(attestationObjectBytes).replace("\n", ""));

        response.setClientDataJSON(generateClientDataJSON(responseFromAPI.getChallenge(), "webauthn.create", "https://admin-ui-test.gluu.org"));
        attestationResultRequest.setResponse(response);
        attestationResult(attestationResultRequest);
        //Toast.makeText(ctx, "attestationObjectBytes : " + attestationObjectBytes.toString(), Toast.LENGTH_SHORT).show();
    }

    public byte[] generateClientDataHash(String challenge, String type, String origin) throws JsonProcessingException, NoSuchAlgorithmException {
        // Convert clientDataJson to JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode clientData = objectMapper.createObjectNode();
        clientData.put("type", type);
        clientData.put("challenge", challenge);
        clientData.put("origin", origin);


        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String serializedClientData = objectMapper.writeValueAsString(clientData);

        // Calculate SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(serializedClientData.getBytes(StandardCharsets.UTF_8));
    }

    public String urlEncodeToString(byte[] src) {
        return Base64.encodeToString(src, Base64.URL_SAFE);
    }

    public String generateClientDataJSON(String challenge, String type, String origin) {


        // Convert clientDataJson to JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode clientData = objectMapper.createObjectNode();
        clientData.put("type", type);
        clientData.put("challenge", challenge);
        clientData.put("origin", origin);
        Log.d(TAG + "clientData.toString()", clientData.toString());
        String clientDataJSON = urlEncodeToString(clientData.toString().getBytes(StandardCharsets.UTF_8));
        Log.d(TAG + "clientDataJSON", clientDataJSON.replace("\n", ""));
        return clientDataJSON.replace("\n", "");
    }

    public void attestationResult(AttestationResultRequest attestationResultRequest) {
        List<FidoConfiguration> fidoConfigurationList = appDatabase.fidoConfigurationDao().getAll();
        if (fidoConfigurationList == null || fidoConfigurationList.isEmpty()) {
            createErrorDialog("Fido configuration not found in database.");
            errorDialog.show();
            //loginButton.setEnabled(false);
            return;
        }
        FidoConfiguration fidoConfiguration = fidoConfigurationList.get(0);
        Call<Map> call = RetrofitClient.getInstance(fidoConfiguration.getIssuer())
                .getAPIInterface().attestationResult(attestationResultRequest,
                        fidoConfiguration.getAttestationResultEndpoint());
        Log.d(TAG, "2============================================================");
        call.enqueue(new Callback<Map>() {
            @Override
            public void onResponse(Call<Map> call, Response<Map> response) {
                Log.d(TAG, "3============================================================");
                Map responseFromAPI = response.body();
                Log.d(TAG, "4============================================================");
                Log.d(TAG, response.message());
                if (response.code() == 200 || response.code() == 201) {
                    Log.d(TAG, "5============================================================");
                    Log.d(TAG, responseFromAPI.toString());
                    Intent intent = null;
                    intent = new Intent(ctx, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                    Log.d(TAG, "6============================================================");
                } else {
                    //return null;
                }
            }

            @Override
            public void onFailure(Call<Map> call, Throwable t) {
                Log.e(TAG, "Inside doDCR :: onFailure :: " + t.getMessage());

            }
        });
    }

    public void assertionOption() {
        List<FidoConfiguration> fidoConfigurationList = appDatabase.fidoConfigurationDao().getAll();
        if (fidoConfigurationList == null || fidoConfigurationList.isEmpty()) {
            createErrorDialog("Fido configuration not found in database.");
            errorDialog.show();
            //loginButton.setEnabled(false);
            return;
        }
        FidoConfiguration fidoConfiguration = fidoConfigurationList.get(0);

        AssertionOptionRequest req = new AssertionOptionRequest();
        req.setUsername("admin");

        Call<AssertionOptionResponse> call = RetrofitClient.getInstance(fidoConfiguration.getIssuer())
                .getAPIInterface().assertionOption(req, fidoConfiguration.getAssertionOptionsEndpoint());

        call.enqueue(new Callback<AssertionOptionResponse>() {
            @Override
            public void onResponse(Call<AssertionOptionResponse> call, Response<AssertionOptionResponse> response) {

                AssertionOptionResponse responseFromAPI = response.body();

                if (response.code() == 200 || response.code() == 201) {
                    if (responseFromAPI.getChallenge() != null) {
                        try {
                            authenticate(responseFromAPI);
                        } catch (VirgilException e) {
                            throw new RuntimeException(e);
                        } catch (WebAuthnException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    //return null;
                }
            }

            @Override
            public void onFailure(Call<AssertionOptionResponse> call, Throwable t) {
                Log.e(TAG, "Inside doDCR :: onFailure :: " + t.getMessage());
            }
        });
    }

    public void authenticate(AssertionOptionResponse responseFromAPI) throws VirgilException, WebAuthnException, NoSuchAlgorithmException, JsonProcessingException {
        AuthenticatorGetAssertionOptions options = new AuthenticatorGetAssertionOptions();
        options.rpId = responseFromAPI.getRpId();
        options.requireUserVerification = false;
        options.requireUserPresence = true;
        options.clientDataHash = generateClientDataHash(responseFromAPI.getChallenge(), "webauthn.get", "https://admin-ui-test.gluu.org");

        List<PublicKeyCredentialDescriptor> allowCredentialDescriptorList = Lists.newArrayList();
        responseFromAPI.getAllowCredentials().stream()
                .forEach(cred -> {

                    Log.d(TAG, cred.getId());
                    PublicKeyCredentialDescriptor publicKeyCredentialDescriptor = new PublicKeyCredentialDescriptor(cred.getType(), decode(cred.getId()), cred.getTransports());
                    allowCredentialDescriptorList.add(publicKeyCredentialDescriptor);
                });

        options.allowCredentialDescriptorList = allowCredentialDescriptorList;


        AuthenticatorGetAssertionResult assertionObject = authenticator.getAssertion(options, new CredentialSelector() {
            @Override
            public PublicKeyCredentialSource selectFrom(List<PublicKeyCredentialSource> credentialList) {
                return credentialList.get(0);
            }
        });

        AssertionResultRequest assertionResultRequest = new AssertionResultRequest();
        assertionResultRequest.setId(urlEncodeToString(assertionObject.selectedCredentialId).replace("\n", "").replace("=", ""));
        assertionResultRequest.setType("public-key");
        assertionResultRequest.setRawId(urlEncodeToString(assertionObject.selectedCredentialId).replace("\n", ""));
        io.jans.chip.modal.Fido.assertion.result.Response response = new io.jans.chip.modal.Fido.assertion.result.Response();
        response.setClientDataJSON(generateClientDataJSON(responseFromAPI.getChallenge(), "webauthn.get", "https://admin-ui-test.gluu.org"));
        response.setAuthenticatorData(urlEncodeToString(assertionObject.authenticatorData).replace("\n", ""));
        response.setSignature(urlEncodeToString(assertionObject.signature).replace("\n", ""));
        assertionResultRequest.setResponse(response);
        assertionResult(assertionResultRequest);
    }

    public void assertionResult(AssertionResultRequest assertionResultRequest) {

        if (fidoConfigurationList == null || fidoConfigurationList.isEmpty()) {
            createErrorDialog("Fido configuration not found in database.");
            errorDialog.show();
            //loginButton.setEnabled(false);
            return;
        }
        FidoConfiguration fidoConfiguration = fidoConfigurationList.get(0);

        if (oidcClientList == null || oidcClientList.isEmpty()) {
            createErrorDialog("OpenID client not found in database.");
            errorDialog.show();
            //loginButton.setEnabled(false);
            return;
        }
        OIDCClient oidcClient = oidcClientList.get(0);

        Log.d(TAG, "=====================assertionResult================================");
        Call<Map> call = RetrofitClient.getInstance(fidoConfiguration.getIssuer())
                .getAPIInterface().assertionResult(assertionResultRequest,
                        fidoConfiguration.getAssertionResultEndpoint());
        call.enqueue(new Callback<Map>() {
            @Override
            public void onResponse(Call<Map> call, Response<Map> response) {
                Map responseFromAPI = response.body();
                Log.d(TAG, "1=====================assertionResult================================");
                Log.d(TAG, response.message());
                if (response.code() == 200 || response.code() == 201) {
                    Log.d(TAG, responseFromAPI.toString());

                    Intent intent = new Intent(ctx, AfterLoginActivity.class);
                    intent.putExtra(USER_INFO, bundle.get(USER_INFO));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);

                }
            }

            @Override
            public void onFailure(Call<Map> call, Throwable t) {
                Log.e(TAG, "Inside doDCR :: onFailure :: " + t.getMessage());

            }
        });
    }

    public byte[] decode(String src) {
        return Base64.decode(src, Base64.URL_SAFE);
    }

    private void createErrorDialog(String message) {
        errorDialog.setMessage(message)
                .setTitle(R.string.error_title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
    }

    private void showErrorDialog(String message) {
        createErrorDialog(message);
        errorDialog.show();
        //loginProgressBar.setVisibility(View.INVISIBLE);
        //loginButton.setEnabled(true);
    }
}
