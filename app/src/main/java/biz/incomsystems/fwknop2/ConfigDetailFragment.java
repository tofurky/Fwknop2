/*
This file is part of Fwknop2.

    Fwknop2 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Foobar is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.incomsystems.fwknop2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Intent;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.sonelli.juicessh.pluginlibrary.PluginContract;

import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * A fragment representing a single Config detail screen.
 * This fragment is either contained in a {@link ConfigListActivity}
 * in two-pane mode (on tablets) or a {@link ConfigDetailActivity}
 * on handsets.
 */
public class ConfigDetailFragment extends Fragment {
    private ConnectionListLoader connectionListLoader;

    OpenPgpServiceConnection myServiceConnection;

    Boolean openKeychainBound = false;
    public static final int REQUEST_CODE_SET_SIG = 9915;
    public static final int REQUEST_CODE_SET_CRYPT = 9916;
    public static final int REQUEST_CODE_PERMISSION = 9917;

    DBHelper mydb;
    Boolean juiceInstalled;
    Boolean openKeychainInstalled;
    PluginContract myJuice;
    Config config;
    TextView txt_NickName ;  // objects representing the config options
    Spinner spn_allowip ;
    Spinner spn_configtype ;
    Spinner spn_ssh ;
    Spinner spn_juice;
    ConnectionSpinnerAdapter juice_adapt;
    TextView txt_allowIP ;
    LinearLayout lay_allowIP;
    LinearLayout lay_natIP;
    LinearLayout lay_natport;
    LinearLayout lay_serverCMD;
    LinearLayout lay_AccessPort;
    LinearLayout lay_fwTimeout;
    LinearLayout lay_sshcmd;
    LinearLayout lay_ovpncmd;
    LinearLayout lay_serverPort;
    LinearLayout lay_KEY;
    LinearLayout lay_gpg_buttons;
    LinearLayout lay_HMAC;
    Button btn_gpg_encrypt;
    Button btn_gpg_sig;
    TextView txt_gpg_crypt;
    TextView txt_gpg_sig;
    TextView txt_ssh_cmd;
    TextView txt_ovpn_cmd;
    String configtype = "Open Port";
    Spinner spn_protocol;
    Spinner spn_DigestType;
    Spinner spn_HMACType;

    TextView txt_ports ;
    TextView txt_server_ip ;
    TextView txt_server_port ;
    TextView txt_server_time ;
    TextView txt_nat_ip;
    TextView txt_nat_port;
    TextView txt_server_cmd;
    TextView txt_KEY ;
    CheckBox chkb64key ;
    TextView txt_HMAC ;
    CheckBox chkb64hmac ;
    CheckBox chkblegacy;
    CheckBox chkbrandom;
    CheckBox chkbKeep_Open;
    CheckBox chkbUse_GPG;

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * active_Nick is our config index.
     */
    private String active_Nick;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ConfigDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = new Config();
        mydb = new DBHelper(getActivity()); // grabbing a database instance
        active_Nick = getArguments().getString(ARG_ITEM_ID); // This populates active_Nick with index of the selected config

        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo("com.sonelli.juicessh", PackageManager.GET_ACTIVITIES);
            juiceInstalled = true;
            Log.v("fwknop2", "juicessh installed");
        } catch (PackageManager.NameNotFoundException e) {
            juiceInstalled = false;
            Log.v("fwknop2", "juicessh not installed");
        }

        try {
            pm.getPackageInfo("org.sufficientlysecure.keychain", PackageManager.GET_ACTIVITIES);
            openKeychainInstalled = true;
            Log.v("fwknop2", "Openkeychain installed");
        } catch (PackageManager.NameNotFoundException e) {
            openKeychainInstalled = false;
            Log.v("fwknop2", "Openkeychain not installed");
        }
        if (openKeychainInstalled) {
            myServiceConnection = new OpenPgpServiceConnection(getActivity().getApplicationContext(), "org.sufficientlysecure.keychain",
                new OpenPgpServiceConnection.OnBound() {
                    @Override
                    public void onBound(IOpenPgpService2 service) {
                        Log.d(OpenPgpApi.TAG, "onBound!");
                        openKeychainBound = true;

                        Intent data = new Intent();
                        data.setAction(OpenPgpApi.ACTION_CHECK_PERMISSION);
                        OpenPgpApi api = new OpenPgpApi(getActivity(), myServiceConnection.getService());

                        Intent result = api.executeApi(data, (InputStream) null, null);
                        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                            case OpenPgpApi.RESULT_CODE_SUCCESS: {

                                Log.d("fwknop2", "Result OK");
                                break;
                            }
                            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                                //Toast.makeText(getActivity(), "Interaction required", Toast.LENGTH_LONG).show();
                                Log.d("fwknop2", "Result Interaction");
                                PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                                try {
                                    getActivity().startIntentSenderForResult(pi.getIntentSender(), 42, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e("fwknop2", "SendIntentException", e);
                                }
                                break;
                            }
                            case OpenPgpApi.RESULT_CODE_ERROR: {
                                //OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                                Toast.makeText(getActivity(), "Opengpg error, is Openkeychain installed?", Toast.LENGTH_LONG).show();
                                chkbUse_GPG.setChecked(false);
                                lay_KEY.setVisibility(View.VISIBLE);
                                chkb64key.setVisibility(View.VISIBLE);
                                lay_gpg_buttons.setVisibility(View.GONE);
                            }

                        }


                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(OpenPgpApi.TAG, "exception when binding!", e);
                    }
                }
            );
            //myServiceConnection.bindToService();
        }






    }

    @Override
    public void onDestroy() {
        if (myServiceConnection != null && openKeychainBound) {
            try {
                myServiceConnection.unbindFromService();
            } catch (Exception e) {
                Log.e("fwknop2", "Error unbinding openkeychain service");
            }
        }
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setMenuVisibility(true);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detail_menu, menu);

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.detail_help) {
            Intent detailIntent = new Intent(getActivity(), HelpActivity.class);
            startActivity(detailIntent);
        } else if (id == R.id.qr_code) {
            try {
                IntentIntegrator.forSupportFragment(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES).initiateScan();
            } catch (Throwable e) {
                Log.e("fwknop2", "Barcode Scan error");
            }
        } else if (id == R.id.save) {
            Context context = getActivity(); // We know we will use a toast, so set it up now
            int duration = Toast.LENGTH_LONG;
            CharSequence text = getString(R.string.saving);
            Toast toast = Toast.makeText(context, text, duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            //We load the entered info into the class, and then call the validate function
            config.MESSAGE_TYPE = configtype;
            if (configtype.equalsIgnoreCase("Open Port")) {//messagetype = configtype
                config.PORTS = txt_ports.getText().toString();
                config.SERVER_TIMEOUT = txt_server_time.getText().toString();
                config.KEEP_OPEN = chkbKeep_Open.isChecked();
                config.SERVER_CMD = "";
                config.NAT_PORT = "";
                config.NAT_IP = "";

            }
            if (configtype.equalsIgnoreCase("Nat Access")) {
                config.NAT_IP = txt_nat_ip.getText().toString();
                config.NAT_PORT = txt_nat_port.getText().toString();
                config.PORTS = txt_ports.getText().toString();
                config.SERVER_TIMEOUT = txt_server_time.getText().toString();
                config.KEEP_OPEN = chkbKeep_Open.isChecked();
                config.SERVER_CMD = "";
            }
            if (configtype.equalsIgnoreCase("Local Nat Access")) {
                config.NAT_IP = "127.0.0.1";
                config.NAT_PORT = txt_nat_port.getText().toString();
                config.PORTS = txt_ports.getText().toString();
                config.SERVER_TIMEOUT = txt_server_time.getText().toString();
                config.KEEP_OPEN = chkbKeep_Open.isChecked();
                config.SERVER_CMD = "";
            }
            if (configtype.equalsIgnoreCase("Server Command")) {
                config.SERVER_CMD = txt_server_cmd.getText().toString();
                config.PORTS = "";
                config.SERVER_TIMEOUT = "";
                config.KEEP_OPEN = false;
                config.NAT_PORT = "";
                config.NAT_IP = "";
            }
            if (spn_allowip.getSelectedItemPosition() == 0) {
                config.ACCESS_IP = "Resolve IP";
            } else if (spn_allowip.getSelectedItemPosition() == 1) {
                config.ACCESS_IP = "0.0.0.0";
            } else if (spn_allowip.getSelectedItemPosition() == 2){
                config.ACCESS_IP = txt_allowIP.getText().toString();
            } else if (spn_allowip.getSelectedItemPosition() == 3) {
                config.ACCESS_IP = "Prompt IP";
            }
            config.NICK_NAME = txt_NickName.getText().toString();
            config.SERVER_IP = txt_server_ip.getText().toString();
            if (chkbrandom.isChecked()) {
             config.SERVER_PORT = "random";
            } else {
                config.SERVER_PORT = txt_server_port.getText().toString();
            }
            config.SSH_CMD = "";
            if (spn_ssh.getSelectedItem().toString().equalsIgnoreCase("SSH Uri")) {
                config.SSH_CMD = txt_ssh_cmd.getText().toString();
                config.juice_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            } else if (spn_ssh.getSelectedItem().toString().equalsIgnoreCase("OpenVPN for Android")) {
                config.SSH_CMD = "ovpn:" + txt_ovpn_cmd.getText().toString();
                config.juice_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            } else if (spn_ssh.getSelectedItem().toString().equalsIgnoreCase("Juicessh")) {
                config.SSH_CMD = "juice:" + juice_adapt.getConnectionName(spn_juice.getSelectedItemPosition());
                config.juice_uuid = juice_adapt.getConnectionId(spn_juice.getSelectedItemPosition());
            } else {
                config.juice_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
                config.SSH_CMD = "";
            }

            if (chkbUse_GPG.isChecked()) {
                config.USE_GPG = true;
                config.KEY = "";
                config.KEY_BASE64 = false;
                config.KEEP_OPEN = false;
                //config.GPG_KEY = txt_gpg_crypt.getText().toString();
                //config.GPG_SIG = txt_gpg_sig.getText().toString();
            } else {
                config.USE_GPG = false;
                config.KEY = txt_KEY.getText().toString();       //key
                config.KEY_BASE64 = chkb64key.isChecked();       //is key b64
                config.GPG_KEY = 0L;
                config.GPG_SIG = 0L;
            }
            config.HMAC = txt_HMAC.getText().toString(); // hmac key
            config.HMAC_BASE64 = chkb64hmac.isChecked();                     //is hmac base64
            config.LEGACY = chkblegacy.isChecked();
            switch (spn_protocol.getSelectedItemPosition()) {
                case 0:
                    config.PROTOCOL = "udp";
                    break;
                case 1:
                    config.PROTOCOL = "tcp";
                    break;
                case 2:
                    config.PROTOCOL = "http";
                    break;
            }
            switch (spn_DigestType.getSelectedItemPosition()) {
                case 0:
                    config.DIGEST_TYPE = "MD5";
                    break;
                case 1:
                    config.DIGEST_TYPE = "SHA1";
                    break;
                case 2:
                    config.DIGEST_TYPE = "SHA256";
                    break;
                case 3:
                    config.DIGEST_TYPE = "SHA384";
                    break;
                case 4:
                    config.DIGEST_TYPE = "SHA512";
                    break;
            }
            switch (spn_HMACType.getSelectedItemPosition()) {
                case 0:
                    config.HMAC_TYPE = "MD5";
                    break;
                case 1:
                    config.HMAC_TYPE = "SHA1";
                    break;
                case 2:
                    config.HMAC_TYPE = "SHA256";
                    break;
                case 3:
                    config.HMAC_TYPE = "SHA384";
                    break;
                case 4:
                    config.HMAC_TYPE = "SHA512";
                    break;
            }


            int isValid = config.Is_Valid();
            if (isValid == 0) {
                mydb.updateConfig(config);
                Activity activity = getActivity();
                if(activity instanceof ConfigListActivity) {//this updates the list for one panel mode
                    ConfigListActivity myactivity = (ConfigListActivity) activity;
                    myactivity.onItemSaved();

                } else {
                    ConfigDetailActivity myactivity = (ConfigDetailActivity) activity;
                    myactivity.onBackPressed();
                }
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                alertDialog.setTitle("Error");
                alertDialog.setMessage(getString(isValid));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }


        } else {
            return false;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override // Handle the qr code or openpgp results
    public void onActivityResult(int requestCode, int resultCode, Intent data) { // Handle the qr code results
        Log.d("fwknop2", "Result: " + resultCode);
        if (data == null)
            return;
        if (requestCode == REQUEST_CODE_SET_CRYPT || requestCode == REQUEST_CODE_SET_SIG || requestCode == REQUEST_CODE_PERMISSION) {
            if (requestCode == REQUEST_CODE_SET_CRYPT) {
                config.GPG_KEY = data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED)[0];
                txt_gpg_crypt.setText(OpenPgpUtils.convertKeyIdToHex(config.GPG_KEY));
            } else if (requestCode == REQUEST_CODE_SET_SIG) {
                config.GPG_SIG = data.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, 0);
                txt_gpg_sig.setText(OpenPgpUtils.convertKeyIdToHex(config.GPG_SIG));
            }
        } else {
            Log.d("fwknop2", requestCode + " " + resultCode);
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if ((scanResult != null) && (scanResult.getContents() != null)) {
                String contents = scanResult.getContents();
                for (String stanzas : contents.split(" ")) {
                    String[] tmp = stanzas.split(":");
                    if (tmp[0].equalsIgnoreCase("KEY_BASE64")) {
                        txt_KEY.setText(tmp[1]);
                        chkb64key.setChecked(true);
                    } else if (tmp[0].equalsIgnoreCase("KEY")) {
                        txt_KEY.setText(tmp[1]);
                        chkb64key.setChecked(false);
                    } else if (tmp[0].equalsIgnoreCase("HMAC_KEY_BASE64")) {
                        txt_HMAC.setText(tmp[1]);
                        chkb64hmac.setChecked(true);
                    } else if (tmp[0].equalsIgnoreCase("HMAC_KEY")) {
                        txt_HMAC.setText(tmp[1]);
                        chkb64hmac.setChecked(false);
                    }
                }// end for loop
            }
        }
    }

    private class openkeychainCallback implements OpenPgpApi.IOpenPgpCallback {
        boolean returnToCiphertextField;
        ByteArrayOutputStream os;
        int requestCode;

        private openkeychainCallback(boolean returnToCiphertextField, ByteArrayOutputStream os, int requestCode) {
            this.returnToCiphertextField = returnToCiphertextField;
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            Log.d("fwknop2", "in callback onreturn");
            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                case OpenPgpApi.RESULT_CODE_SUCCESS: {

                    switch (requestCode) {
                        case REQUEST_CODE_SET_CRYPT: {
                            Log.d("fwknop2", "Trying to set crypt");
                            txt_gpg_crypt.setText(OpenPgpUtils.convertKeyIdToHex(result.getLongArrayExtra(OpenPgpApi.RESULT_KEY_IDS)[0]));
                            break;
                        }
                        case REQUEST_CODE_SET_SIG: {
                            txt_gpg_sig.setText(OpenPgpUtils.convertKeyIdToHex(result.getLongArrayExtra(OpenPgpApi.RESULT_KEY_IDS)[0]));
                            break;
                        }
                        default: {

                        }
                    }

                    break;
                }

                case OpenPgpApi.RESULT_CODE_ERROR: {
                    Log.e("fwknop2", "Openkeychain error");

                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    Log.d("fwknop2", "interaction");
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    try {
                        getActivity().startIntentSenderForResult(
                                pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("fwknop2", "Openkeychain start intent error");
                    }
                    break;
                }
            }
        }
    }

    @Override  //This is all the setup stuff for this fragment.
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override public void uncaughtException(Thread t, Throwable e) {
                        Log.e("fwknop2", t.getName()+": "+e);
                        //MyWorker worker = new MyWorker();
                        //worker.start();
                    }
                });

        View rootView = inflater.inflate(R.layout.fragment_config_detail, container, false);
        active_Nick = getArguments().getString("item_id");
        try {
            myJuice = new PluginContract();
        } catch (Throwable e) {
            Log.e("fwknop2", "Juicessh exception");
        }

        //Handlers for the input fields
        txt_NickName = (TextView) rootView.findViewById(R.id.NickName);
        txt_allowIP = (TextView) rootView.findViewById(R.id.iptoallow);
        lay_allowIP = (LinearLayout) rootView.findViewById(R.id.iptoallowsl);
        lay_natIP = (LinearLayout) rootView.findViewById(R.id.natipsl);
        lay_natport = (LinearLayout) rootView.findViewById(R.id.natportsl);
        lay_serverCMD = (LinearLayout) rootView.findViewById(R.id.servercmdsl);
        lay_serverPort = (LinearLayout) rootView.findViewById(R.id.destPortl);
        txt_ports = (TextView) rootView.findViewById(R.id.AccessPorts);
        txt_server_ip = (TextView) rootView.findViewById(R.id.destIP);
        txt_server_port = (TextView) rootView.findViewById(R.id.destPort);
        txt_server_time = (TextView) rootView.findViewById(R.id.fwTimeout);
        txt_server_cmd = (TextView) rootView.findViewById(R.id.servercmd);
        txt_nat_ip = (TextView) rootView.findViewById(R.id.natip);
        txt_nat_port = (TextView) rootView.findViewById(R.id.natport);
        lay_AccessPort = (LinearLayout) rootView.findViewById(R.id.AccessPortsl);
        lay_fwTimeout = (LinearLayout) rootView.findViewById(R.id.fwTimeoutl);
        lay_sshcmd = (LinearLayout) rootView.findViewById(R.id.sshcmdsl);
        txt_ssh_cmd = (TextView) rootView.findViewById(R.id.sshcmd);
        lay_ovpncmd = (LinearLayout) rootView.findViewById(R.id.ovpncmdsl);
        txt_ovpn_cmd = (TextView) rootView.findViewById(R.id.ovpncmd);
        lay_KEY = (LinearLayout) rootView.findViewById(R.id.passwdl);
        lay_gpg_buttons = (LinearLayout) rootView.findViewById(R.id.lay_gpg_buttons);
        btn_gpg_encrypt = (Button) rootView.findViewById(R.id.gpg_encrypt_button);
        btn_gpg_sig = (Button) rootView.findViewById(R.id.gpg_sig_button);

        txt_gpg_crypt = (TextView) rootView.findViewById(R.id.gpg_encrypt_lbl);
        txt_gpg_sig = (TextView) rootView.findViewById(R.id.gpg_sig_lbl);

        txt_KEY = (TextView) rootView.findViewById(R.id.passwd);
        lay_HMAC = (LinearLayout) rootView.findViewById(R.id.hmacl);
        txt_HMAC = (TextView) rootView.findViewById(R.id.hmac);
        chkb64hmac = (CheckBox) rootView.findViewById(R.id.chkb64hmac);
        chkb64key = (CheckBox) rootView.findViewById(R.id.chkb64key);
        chkblegacy = (CheckBox) rootView.findViewById(R.id.chkblegacy);
        chkbrandom = (CheckBox) rootView.findViewById(R.id.chkbrandom);
        spn_allowip = (Spinner) rootView.findViewById(R.id.allowip);
        chkbKeep_Open = (CheckBox) rootView.findViewById(R.id.chkbKeep_open);
        chkbUse_GPG = (CheckBox) rootView.findViewById(R.id.chkbGPG);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.spinner_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_allowip.setAdapter(adapter);
        spn_allowip.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 2) {
                    lay_allowIP.setVisibility(View.VISIBLE);
                } else {
                    lay_allowIP.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        chkbrandom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {
                if (ischecked) {
                    lay_serverPort.setVisibility(View.GONE);
                } else {
                    lay_serverPort.setVisibility(View.VISIBLE);
                }
            }
        });

        chkbUse_GPG.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {
                if (ischecked) {
                    if (!openKeychainInstalled) {
                        Toast.makeText(getActivity(), "Openkeychain must be installed to use GPG", Toast.LENGTH_LONG).show();
                        chkbUse_GPG.setChecked(false);
                        return;
                    }
                    Intent data = new Intent();
                    data.setAction(OpenPgpApi.ACTION_CHECK_PERMISSION);
                    OpenPgpApi api = new OpenPgpApi(getActivity(), myServiceConnection.getService());
                    api.executeApiAsync(data, null, null, new openkeychainCallback(false, null, REQUEST_CODE_PERMISSION));

                    lay_KEY.setVisibility(View.GONE);
                    chkb64key.setVisibility(View.GONE);
                    lay_gpg_buttons.setVisibility(View.VISIBLE);
                    chkbKeep_Open.setVisibility(View.GONE);
                    myServiceConnection.bindToService();

                } else {
                    lay_KEY.setVisibility(View.VISIBLE);
                    chkbKeep_Open.setVisibility(View.VISIBLE);
                    chkb64key.setVisibility(View.VISIBLE);
                    lay_gpg_buttons.setVisibility(View.GONE);
                }
            }
        });
        btn_gpg_encrypt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //move this to the buttons
                Intent data = new Intent();
                data.setAction(OpenPgpApi.ACTION_GET_KEY_IDS);
                data.putExtra(OpenPgpApi.EXTRA_USER_IDS, new String[]{""});
                OpenPgpApi api = new OpenPgpApi(getActivity(), myServiceConnection.getService());
                api.executeApiAsync(data, null, null, new openkeychainCallback(false, null, REQUEST_CODE_SET_CRYPT));
            }
        });
        btn_gpg_sig.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //move this to the buttons
                Intent data = new Intent();
                data.setAction(OpenPgpApi.ACTION_GET_SIGN_KEY_ID);
                data.putExtra(OpenPgpApi.EXTRA_USER_ID, new String[]{""});
                OpenPgpApi api = new OpenPgpApi(getActivity(), myServiceConnection.getService());
                api.executeApiAsync(data, null, null, new openkeychainCallback(false, null, REQUEST_CODE_SET_SIG));
            }
        });




        spn_protocol = (Spinner) rootView.findViewById(R.id.spn_protocol);
        ArrayAdapter<CharSequence> adapter_protocol = ArrayAdapter.createFromResource(getActivity(),
                R.array.spinner_protocol, android.R.layout.simple_spinner_item);
        spn_protocol.setAdapter(adapter_protocol);
        adapter_protocol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spn_DigestType = (Spinner) rootView.findViewById(R.id.spn_DigestType);
        ArrayAdapter<CharSequence> adapter_DigestType = ArrayAdapter.createFromResource(getActivity(),
                R.array.spinner_digest_type, android.R.layout.simple_spinner_item);
        spn_DigestType.setAdapter(adapter_DigestType);
        adapter_protocol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_DigestType.setSelection(2);

        spn_HMACType = (Spinner) rootView.findViewById(R.id.spn_HMACType);
        ArrayAdapter<CharSequence> adapter_HMAC_Type = ArrayAdapter.createFromResource(getActivity(),
                R.array.spinner_digest_type, android.R.layout.simple_spinner_item);
        spn_HMACType.setAdapter(adapter_HMAC_Type);
        adapter_protocol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_HMACType.setSelection(2);

        spn_ssh = (Spinner) rootView.findViewById(R.id.ssh);
        ArrayList<String> list = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.ssh_options)));
        ArrayAdapter<String> adapter_ssh = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item,list);
        adapter_ssh.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_ssh.setAdapter(adapter_ssh);
        spn_juice = (Spinner) rootView.findViewById(R.id.juice_spn);

        if (juiceInstalled) {
            int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                    "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS");
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{"com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS", "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"},
                        0);
            }
            try {
                juice_adapt = new ConnectionSpinnerAdapter(getActivity());
                spn_juice.setAdapter(juice_adapt);
            } catch (Throwable ex) {
                Log.e("fwknop2", "Juicessh error");
            }
        } else {
            list.remove(3);
            adapter_ssh.notifyDataSetChanged();
        }

        spn_ssh.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                // parent.getItemAtPosition(pos)
                if (pos == 0) {
                    lay_sshcmd.setVisibility(View.GONE);
                    lay_ovpncmd.setVisibility(View.GONE);
                    spn_juice.setVisibility(View.GONE);
                    // blank the other options here
                } else if (pos == 1) {
                    // show the txt for the uri
                    lay_sshcmd.setVisibility(View.VISIBLE);
                    lay_ovpncmd.setVisibility(View.GONE);
                    spn_juice.setVisibility(View.GONE);
                } else if (pos == 3) {
                    if (getActivity().checkCallingOrSelfPermission("com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS") == PackageManager.PERMISSION_GRANTED) {
                        try {
                            lay_sshcmd.setVisibility(View.GONE);
                            lay_ovpncmd.setVisibility(View.GONE);

                            if (connectionListLoader == null) {

                                connectionListLoader = new ConnectionListLoader(getActivity(), juice_adapt);
                                connectionListLoader.setOnLoadedListener(new ConnectionListLoader.OnLoadedListener() {
                                    @Override
                                    public void onLoaded() {  // This is so ugly...
                                        spn_juice.setVisibility(View.VISIBLE);
                                        if (config.SSH_CMD.contains("juice:") && spn_juice.getCount() > 0) {
                                            for (int n = 0; n < spn_juice.getCount(); n++) {
                                                if (config.SSH_CMD.contains(juice_adapt.getConnectionName(n))) {
                                                    spn_juice.setSelection(n);
                                                }
                                            }
                                        }
                                    }
                                });
                                getActivity().getSupportLoaderManager().initLoader(0, null, connectionListLoader);
                            } else {
                                getActivity().getSupportLoaderManager().restartLoader(0, null, connectionListLoader);
                            }
                        } catch (Throwable e) {
                            Log.e("fwknop2", "Juicessh error");
                        }
                    } else {
                        Context context = getActivity();
                        CharSequence text = getText(R.string.juice_permissions);
                        if(Build.VERSION.SDK_INT < 23) {
                            text = getText(R.string.juice_permissions_reinstall);
                        }
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                } else if (pos == 2) {
                    // show the txt for the uri
                    lay_sshcmd.setVisibility(View.GONE);
                    lay_ovpncmd.setVisibility(View.VISIBLE);
                    spn_juice.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spn_configtype = (Spinner) rootView.findViewById(R.id.configtype);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getActivity(),
                R.array.configtype_options, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_configtype.setAdapter(adapter2);
        spn_configtype.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos== 0) {
                    configtype = "Open Port";
                    lay_AccessPort.setVisibility(View.VISIBLE);
                    lay_fwTimeout.setVisibility(View.VISIBLE);
                    if (!chkbUse_GPG.isChecked())
                        chkbKeep_Open.setVisibility(View.VISIBLE);
                    lay_natIP.setVisibility(View.GONE);
                    lay_natport.setVisibility(View.GONE);
                    lay_serverCMD.setVisibility(View.GONE);
                    txt_nat_ip.setText("");
                    txt_nat_port.setText("");
                    txt_server_cmd.setText("");
                } else if (pos == 1) {
                    configtype = "Nat Access";
                    lay_AccessPort.setVisibility(View.VISIBLE);
                    lay_fwTimeout.setVisibility(View.VISIBLE);
                    if (!chkbUse_GPG.isChecked())
                        chkbKeep_Open.setVisibility(View.VISIBLE);
                    lay_natIP.setVisibility(View.VISIBLE);
                    lay_natport.setVisibility(View.VISIBLE);
                    lay_serverCMD.setVisibility(View.GONE);
                    txt_server_cmd.setText("");
                } else if (pos == 2) {
                    configtype = "Local Nat Access";
                    lay_AccessPort.setVisibility(View.VISIBLE);
                    lay_fwTimeout.setVisibility(View.VISIBLE);
                    if (!chkbUse_GPG.isChecked())
                        chkbKeep_Open.setVisibility(View.VISIBLE);
                    lay_natIP.setVisibility(View.GONE);
                    lay_natport.setVisibility(View.VISIBLE);
                    lay_serverCMD.setVisibility(View.GONE);
                    txt_server_cmd.setText("");
                    txt_nat_ip.setText("127.0.0.1");
                } else if (pos == 3) {
                    configtype = "Server Command";
                    lay_AccessPort.setVisibility(View.GONE);
                    lay_fwTimeout.setVisibility(View.GONE);
                    chkbKeep_Open.setVisibility(View.GONE);
                    lay_natIP.setVisibility(View.GONE);
                    lay_natport.setVisibility(View.GONE);
                    lay_serverCMD.setVisibility(View.VISIBLE);
                    txt_ports.setText("");
                    txt_nat_ip.setText("");
                    txt_nat_port.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //Below is the loading of a saved config
        if (active_Nick.equalsIgnoreCase("")) {
            txt_NickName.setText("");
            config.SSH_CMD = "";
            txt_HMAC.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            txt_KEY.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            config = mydb.getConfig(active_Nick);
            txt_NickName.setText(active_Nick);
            if (config.ACCESS_IP.equalsIgnoreCase( "Resolve IP")) {
                spn_allowip.setSelection(0);
            } else if (config.ACCESS_IP.equalsIgnoreCase("0.0.0.0")) {
                spn_allowip.setSelection(1);
            } else if (config.ACCESS_IP.equalsIgnoreCase("Prompt IP")) {
                spn_allowip.setSelection(3);
            } else {
                spn_allowip.setSelection(2);
                txt_allowIP.setText(config.ACCESS_IP);
            }
            txt_ports.setText(config.PORTS);
            txt_server_ip.setText(config.SERVER_IP);
            txt_server_port.setText(config.SERVER_PORT); // check for random
            if (config.SERVER_PORT.equalsIgnoreCase("random")){
                chkbrandom.setChecked(true);
            }
            txt_server_time.setText(config.SERVER_TIMEOUT);
            chkbKeep_Open.setChecked(config.KEEP_OPEN);

            if (config.GPG_KEY == 0) {
                config.USE_GPG = false;
                chkbUse_GPG.setChecked(false);
                chkbKeep_Open.setVisibility(View.VISIBLE);
                txt_KEY.setText(config.KEY);
                lay_KEY.setVisibility(View.VISIBLE);
                chkb64key.setVisibility(View.VISIBLE);
                lay_gpg_buttons.setVisibility(View.GONE);
                if (config.KEY_BASE64) {
                    chkb64key.setChecked(true);
                } else {
                    chkb64key.setChecked(false);
                }


            } else {
                chkbUse_GPG.setChecked(true);
                config.USE_GPG = true;
                Log.d("fwknop2", "here");
                chkbKeep_Open.setVisibility(View.GONE);
                lay_KEY.setVisibility(View.GONE);
                chkb64key.setVisibility(View.GONE);
                lay_gpg_buttons.setVisibility(View.VISIBLE);
                txt_KEY.setText("");
                txt_gpg_crypt.setText(OpenPgpUtils.convertKeyIdToHex(config.GPG_KEY));
                txt_gpg_sig.setText(OpenPgpUtils.convertKeyIdToHex(config.GPG_SIG));
            }
            txt_HMAC.setText(config.HMAC);
            if (config.HMAC_BASE64) {
                chkb64hmac.setChecked(true);
            } else {
                chkb64hmac.setChecked(false);
            }
            if (!config.SERVER_CMD.equalsIgnoreCase("")) { //can move this logic elsewhere
                spn_configtype.setSelection(3);
                txt_server_cmd.setText(config.SERVER_CMD);
            } else if (!config.NAT_IP.equalsIgnoreCase("")) {
                if (config.NAT_IP.equalsIgnoreCase("127.0.0.1")) {
                    spn_configtype.setSelection(2);

                } else {
                    spn_configtype.setSelection(1);
                }
                txt_ports.setText(config.PORTS);
                txt_nat_ip.setText(config.NAT_IP);
                txt_nat_port.setText(config.NAT_PORT);
                txt_server_time.setText(config.SERVER_TIMEOUT);
            } else {
                spn_configtype.setSelection(0);
            }
            if (config.SSH_CMD.equalsIgnoreCase("")) {
                spn_ssh.setSelection(0);
            } else if (config.SSH_CMD.contains("juice:") && juiceInstalled) {
                spn_ssh.setSelection(3);
            } else if (config.SSH_CMD.contains("ovpn:")) {
                spn_ssh.setSelection(2);
                txt_ovpn_cmd.setText(config.SSH_CMD.substring(5));
            } else {
                spn_ssh.setSelection(1);
                txt_ssh_cmd.setText(config.SSH_CMD);
            }
                chkblegacy.setChecked(config.LEGACY);
            if(config.PROTOCOL.equalsIgnoreCase("tcp")) {
                spn_protocol.setSelection(1);
            } else if(config.PROTOCOL.equalsIgnoreCase("http")) {
                spn_protocol.setSelection(2);
            } else {
                spn_protocol.setSelection(0);
            }
            if (config.DIGEST_TYPE.equalsIgnoreCase("MD5")) {
                spn_DigestType.setSelection(0);
            } else if (config.DIGEST_TYPE.equalsIgnoreCase("SHA1")) {
                spn_DigestType.setSelection(1);
            } else if (config.DIGEST_TYPE.equalsIgnoreCase("SHA256")) {
                spn_DigestType.setSelection(2);
            } else if (config.DIGEST_TYPE.equalsIgnoreCase("SHA384")) {
                spn_DigestType.setSelection(3);
            } else if (config.DIGEST_TYPE.equalsIgnoreCase("SHA512")) {
                spn_DigestType.setSelection(4);
            }

            if (config.HMAC_TYPE.equalsIgnoreCase("MD5")) {
                spn_HMACType.setSelection(0);
            } else if (config.HMAC_TYPE.equalsIgnoreCase("SHA1")) {
                spn_HMACType.setSelection(1);
            } else if (config.HMAC_TYPE.equalsIgnoreCase("SHA256")) {
                spn_HMACType.setSelection(2);
            } else if (config.HMAC_TYPE.equalsIgnoreCase("SHA384")) {
                spn_HMACType.setSelection(3);
            } else if (config.HMAC_TYPE.equalsIgnoreCase("SHA512")) {
                spn_HMACType.setSelection(4);
            }
        }
        return rootView;
    }
}

