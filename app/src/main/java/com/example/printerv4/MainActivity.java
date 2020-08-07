package com.example.printerv4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Connection connection;

    private RadioButton btRadioButton;
    private String macAddressEditText;
    private Spinner macAddressDropdown;
    //private EditText ipAddressEditText;
    //private EditText portNumberEditText;
    private static final String bluetoothAddressKey = "ZEBRA_DEMO_BLUETOOTH_ADDRESS";
    //private static final String tcpAddressKey = "ZEBRA_DEMO_TCP_ADDRESS";
    //private static final String tcpPortKey = "ZEBRA_DEMO_TCP_PORT";
    private static final String PREFS_NAME = "OurSavedAddress";

    private Button printButton;
    private Button refreshButton;
    private Button connectButton;
    private Button printAllButton;
    private ZebraPrinter printer;
    private TextView statusField;
    private Spinner printManyLabels; //spinner dropdown to select how many to print
    private Spinner jobDropdown; //spinner dropdown to select the job you wish to print the samples of from the google sheet
    private Spinner boreholeDropdown;//spinner dropdown to select which borehole sheet containing samples
    private Spinner sampleDropdown;//spinner dropdown to actually choose which sample from the borehole sheet
    private Spinner sampleFilterDropdown; //spinner dropwdown that lets you select which sample type to retrieve from the google sheet (acting as a filter of sorts)

    int itemPosition;//this is for the testbuttonMany dropdown (dictates how many samples u want to print based on the position on the spinner dropdown u select)

    ArrayList<String> spreadsheetIDs = new ArrayList<>();
    ArrayList<String> projectNames = new ArrayList<>();
    ArrayList<String> jobRows = new ArrayList<>();//an arraylist that contains each row of the job database
    ArrayList<String> boreholes = new ArrayList<>();//the purpose of this is to receive the boreholes of the lines arraylist once I split it

    ReadCSVJobs readJobs = new ReadCSVJobs();
    ArrayAdapter<String> jobsAdapter;
    ArrayAdapter<String> boreholesAdapter;
    ArrayAdapter<String> sampleFilterAdapter;
    ArrayAdapter<String> samplesAdapter;

    ArrayAdapter<String> macAdapter;

    String spreadsheetID;
    String boreholeName;

    String sampleFilter;

    ArrayList<String> sampleRows = new ArrayList<>();
    ArrayList<String> sampleNos = new ArrayList<>();
    ArrayList<String> sampleTypes = new ArrayList<>(); //an arraylist of sample types that will be called from a method from the readCSVSample class

    String projectNoAndName;
    String sampleNo;
    String location;
    String sampleType;
    String topDepth;
    String bottomDepth;
    String samplingDate;
    String qrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        /*
        ipAddressEditText = this.findViewById(R.id.ipAddressInput);
        String ip = settings.getString(tcpAddressKey, "");
        ipAddressEditText.setText(ip);

        portNumberEditText = this.findViewById(R.id.portInput);
        String port = settings.getString(tcpPortKey, "");
        portNumberEditText.setText(port);
        */

        macAddressDropdown = findViewById(R.id.macInput);
        macAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SettingsHelper.getBluetoothName());
        macAddressDropdown.setAdapter(macAdapter);
        macAddressDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            protected Adapter initializedAdapter = null;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Always ignore the initial selection performed after setAdapter
                if( initializedAdapter !=jobDropdown.getAdapter() ) {
                    initializedAdapter = jobDropdown.getAdapter();
                    return;
                }
                macAddressEditText = SettingsHelper.getBluetoothAddress().get(macAddressDropdown.getSelectedItemPosition());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //String mac = settings.getString(bluetoothAddressKey, "54:6C:0E:01:7F:C8");
        //macAddressEditText.setText(mac);

        statusField = this.findViewById(R.id.statusText);

        //toggleEditField(macAddressEditText, true);


        //btRadioButton = this.findViewById(R.id.bluetoothRadio);


       // RadioGroup radioGroup = this.findViewById(R.id.radioGroup);
       // radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

           // public void onCheckedChanged(RadioGroup group, int checkedId) {
                //if (checkedId == R.id.bluetoothRadio) {
                    //toggleEditField(macAddressEditText, true);
                    //toggleEditField(portNumberEditText, false);
                    //toggleEditField(ipAddressEditText, false);
                //} else {
                    //toggleEditField(portNumberEditText, true);
                    //toggleEditField(ipAddressEditText, true);
                    //toggleEditField(macAddressEditText, false);
                //}
            //}
       // });

        connectButton = this.findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printer = connect();
            }
        });

        refreshButton = this.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        printButton = this.findViewById(R.id.testButton);
        printButton.setEnabled(false);
        printButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        //enableTestButtons(true);
                        Looper.prepare();
                        doConnectionTest();
                        Looper.loop();
                        Looper.myLooper().quit();
                    }
                }).start();
            }
        });

        printAllButton = this.findViewById(R.id.printAllSamplesButton);
        printAllButton.setEnabled(false);


        //get the spinner from the xml.
        printManyLabels = findViewById(R.id.printManyLabels);
        //create a list of items for the spinner.
        String[] numberOptions = new String[]{"1", "2", "3", "4", "5"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> printManyLabelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, numberOptions);
        //set the spinners adapter to the previously created one.
        printManyLabels.setAdapter(printManyLabelsAdapter);
        printManyLabels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                itemPosition = printManyLabels.getSelectedItemPosition()+1;

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        readJobs.start(); //start new thread which handles reading the data from the CSV file
        jobRows = readJobs.getLines();
        jobRows.add("");


        jobDropdown = findViewById(R.id.jobDropdown);
        projectNames = readJobs.getProjectNames();
        projectNames.add("Select..."); //for some reason, YOU NEED to add this extra item to the arraylist in order for it to actually select items from the dropdown
                                              //it does not work if you do not do this (!!)
                                            //ALSO, note that when you click on the dropdown, all the retrieved CSV fields are in double quotation marks ""
                                            //FIND OUT WHY

        jobsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, projectNames);
        jobDropdown.setAdapter(jobsAdapter);
        jobDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            protected Adapter initializedAdapter = null;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Always ignore the initial selection performed after setAdapter
                if( initializedAdapter !=jobDropdown.getAdapter() ) {
                    initializedAdapter = jobDropdown.getAdapter();
                    return;
                }
                jobsAdapter.notifyDataSetChanged();

                printButton.setEnabled(false);

                boreholes.clear();
                boreholes.add("Select...");
                sampleNos.clear();

                spreadsheetIDs = readJobs.getSpreadsheetIDs();
                if(!jobDropdown.getSelectedItem().toString().equals("Select...")) {
                    spreadsheetID = spreadsheetIDs.get(jobDropdown.getSelectedItemPosition() - 1);//this gets the spreadsheetID for the job that you select and assigns it to the spreadsheetID variable
                }

                boreholesAdapter.notifyDataSetChanged();//if you want to change the job select, you notify that the data set of the borehole dropdown has changed so you can update the textview of the borehole dropdown
                samplesAdapter.notifyDataSetChanged();

                String[] boreholesArray = jobRows.get(jobDropdown.getSelectedItemPosition()).split(",");//creates an array for the purpose of storing the WHOLE row of the database that contains the job name that the user selects from the job dropdown list to it and then splitting the line up and iterating through it, adding each individual borehole name to the boreholes arraylist so that it can be made into the correct borehole dropdown
                for (int j = 2; j <boreholesArray.length; j++){//we start from j=2 because the first two columns of the job database are project name and spreadsheet id which we dont need, so we start from the 3rd column onwards, which will have all the borehole names.
                    boreholes.add(boreholesArray[j].replace("\"",""));//adds all the boreholes of the Job sheet to the boreholes arraylist and replaces all occurunces of " with nothing
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        boreholeDropdown = findViewById(R.id.boreholeDropdown);
        boreholesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, boreholes);
        boreholeDropdown.setAdapter(boreholesAdapter);
        boreholeDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            protected Adapter initializedAdapter = null;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Always ignore the initial selection performed after setAdapter
                if( initializedAdapter !=jobDropdown.getAdapter() ) {
                    initializedAdapter = jobDropdown.getAdapter();
                    return;
                }

                sampleNos.clear();

                printButton.setEnabled(false);

                samplesAdapter.notifyDataSetChanged();
                boreholeName = boreholeDropdown.getSelectedItem().toString();//set the boreholeName to whatever borehole the user picked so that this name can then be used in retrieving the right google sheet with samples
                changeDeets();


            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //here you need to add the final sample dropdown which displays all the samples gathered in the sampleRows above ^
        samplesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sampleNos);




    }

    public void changeDeets(){
        final ReadCSVSample readSamples = new ReadCSVSample(spreadsheetID, boreholeName);
        //the fact that this is made final is important as it means that if you assign it to anything (i.e. with an equals sign)
        //then, it will alsways stay this way no matter what, even if you do .clear()


        readSamples.start();

        sampleFilterDropdown = findViewById(R.id.sampleFilterDropdown);
        //create a list of items for the spinner.
        final String[] sampleTypeOptions = new String[]{"Select...","Any","Environmental","Geotechnical"};
        sampleFilterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sampleTypeOptions);
        sampleFilterDropdown.setAdapter(sampleFilterAdapter);
        sampleFilterDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            protected Adapter initializedAdapter = null;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Always ignore the initial selection performed after setAdapter
                if( initializedAdapter !=jobDropdown.getAdapter() ) {
                    initializedAdapter = jobDropdown.getAdapter();
                    return;
                }
                sampleNos.clear();
                sampleRows.clear();

                sampleTypes = readSamples.getSampleTypes();
                int loopNum = sampleTypes.size();

                printButton.setEnabled(false);
                sampleFilter = sampleFilterDropdown.getSelectedItem().toString();

                /*
                it used to be like this:
                if(sampleFilter.equals("Any")){
                    sampleRows = readSamples.getSampleRows;
                    sampleNos = readSamples.getSampleNos;
                }
                the reason i changed it is due the fact of me adding the sampleNos.clear() and sampleRows.clear() up above.
                My theory is that because the class ReadCSVSample has to be final it means that when you assign something, it will always stay that way,
                 (so in the above if statement, you assign the arraylist from the ReadCSVSample class to the arraylist in this class and thus create a constant link between the two)
                so, before when you clicked "Any" it assigned the value, and if you tried to then change from "any" to "environmental" or "geotechnical", an error occured giving an indexOutOfBounds
                 exception claiming the arrayList in the ReadCSVSample class is of size 0, which shouldnt be true. But, the reason for this is because you created the link between arraylists
                 by equating the two from each class, so when you did the .clear() on the arraylists in this class, it also cleared the arraylists in the ReadCSV class, which is what created the error
                */
                if(sampleFilter.equals("Any")){
                    for (int j = 0; j<loopNum; j++){
                        sampleRows.add(readSamples.getSampleRows().get(j));//this gets the sampleRows from the ReadCSVSample class which contains every row of sample information of the chosen borehole
                        sampleNos.add(readSamples.getSampleNos().get(j));
                    }
                }
                else if(sampleFilter.equals("Environmental")){
                    for (int j = 0; j<loopNum; j++){
                        if (sampleTypes.get(j).equals("ES") || sampleTypes.get(j).equals("EW")){
                            sampleRows.add(readSamples.getSampleRows().get(j));
                            sampleNos.add(readSamples.getSampleNos().get(j));
                        }
                    }
                }
                else if(sampleFilter.equals("Geotechnical")){
                    for (int j =0; j<loopNum; j++){
                        if (!sampleTypes.get(j).equals("ES") && !sampleTypes.get(j).equals("EW")){
                            sampleRows.add(readSamples.getSampleRows().get(j));
                            sampleNos.add(readSamples.getSampleNos().get(j));
                        }
                    }
                }

                changeDets();

            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    public void changeDets(){

        sampleDropdown = findViewById(R.id.sampleDropdown);
        sampleNos.add(0, "Select...");
        sampleNos.add(1, "Print All Samples");
        samplesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sampleNos);
        sampleDropdown.setAdapter(samplesAdapter);
        sampleDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            protected Adapter initializedAdapter = null;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Always ignore the initial selection performed after setAdapter
                if( initializedAdapter !=jobDropdown.getAdapter() ) {
                    initializedAdapter = jobDropdown.getAdapter();
                    return;
                }


                printManyLabels.setEnabled(true);

                samplesAdapter.notifyDataSetChanged();

                if(sampleDropdown.getSelectedItemPosition() == 1){
                    printButton.setEnabled(false);
                    printAllButton.setEnabled(true);
                    printAll();
                }
                else{
                    printAllButton.setEnabled(false);
                    printButton.setEnabled(true);
                    String[] sampleData = sampleRows.get(sampleDropdown.getSelectedItemPosition()-2).split(","); //this takes a whole row of info about the sample that the user chose from a google sheet and splits it
                    projectNoAndName = sampleData[0].replace("\"", "");
                    sampleNo = sampleData[1].replace("\"", "");
                    location = sampleData[2].replace("\"", "");
                    sampleType = sampleData[3].replace("\"", "");
                    topDepth = sampleData[4].replace("\"", "");
                    bottomDepth = sampleData[5].replace("\"", "");
                    samplingDate = sampleData[6].replace("\"", "");
                    qrCode = sampleData[7].replace("\"", "");
                }



            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    public void printAll(){

        printAllButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        for (int j = 0; j < sampleRows.size(); j++){
                            String[] sampleData = sampleRows.get(j).split(",");
                            projectNoAndName = sampleData[0].replace("\"", "");
                            sampleNo = sampleData[1].replace("\"", "");
                            location = sampleData[2].replace("\"", "");
                            sampleType = sampleData[3].replace("\"", "");
                            topDepth = sampleData[4].replace("\"", "");
                            bottomDepth = sampleData[5].replace("\"", "");
                            samplingDate = sampleData[6].replace("\"", "");
                            qrCode = sampleData[7].replace("\"", "");

                            doConnectionTest();

                        }

                    }
                }).start();
            }
        });


    }



    public ZebraPrinter connect() {
        setStatus("Connecting...", Color.YELLOW);
        connection = null;
        connection = new BluetoothConnection(getMacAddressFieldText());
        SettingsHelper.saveBluetoothAddress(this, getMacAddressFieldText());

        /*
        else {
            try {
                int port = Integer.parseInt(getTcpPortNumber());
                connection = new TcpConnection(getTcpAddress(), port);
                SettingsHelper.saveIp(this, getTcpAddress());
                SettingsHelper.savePort(this, getTcpPortNumber());
            } catch (NumberFormatException e) {
                setStatus("Port Number Is Invalid", Color.RED);
                return null;
            }
        }
        */

        try {
            connection.open();
            setStatus("Connected", Color.GREEN);
        } catch (ConnectionException e) {
            setStatus("Comm Error! Disconnecting", Color.RED);
            DemoSleeper.sleep(1000);
            disconnect();
        }

        ZebraPrinter printer = null;

        if (connection.isConnected()) {
            try {
                printer = ZebraPrinterFactory.getInstance(connection);
                setStatus("Determining Printer Language", Color.YELLOW);
                String pl = SGD.GET("device.languages", connection);
                setStatus("Printer Language " + pl, Color.BLUE);
            } catch (ConnectionException | ZebraPrinterLanguageUnknownException e) {
                setStatus("Unknown Printer Language", Color.RED);
                printer = null;
                DemoSleeper.sleep(1000);
                disconnect();
            }
        }
        return printer;
    }

    public void disconnect() {
        try {
            setStatus("Disconnecting", Color.RED);
            if (connection != null) {
                connection.close();
            }
            setStatus("Not Connected", Color.RED);
        } catch (ConnectionException e) {
            setStatus("COMM Error! Disconnected", Color.RED);
        } finally {
            //enableTestButtons(true);
        }
    }

    private void setStatus(final String statusMessage, final int color) {
        runOnUiThread(new Runnable() {
            public void run() {
                statusField.setBackgroundColor(color);
                statusField.setText(statusMessage);
            }
        });
        DemoSleeper.sleep(1000);
    }


    private void sendTestLabel() {
        try {
            ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);

            PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();

            if (printerStatus.isReadyToPrint) {
                byte[] configLabel = getConfigLabel();
                connection.write(configLabel);
                setStatus("Sending Data", Color.BLUE);
            } else if (printerStatus.isHeadOpen) {
                setStatus("Printer Head Open", Color.RED);
            } else if (printerStatus.isPaused) {
                setStatus("Printer is Paused", Color.RED);
            } else if (printerStatus.isPaperOut) {
                setStatus("Printer Media Out", Color.RED);
            }
            DemoSleeper.sleep(1500);
            if (connection instanceof BluetoothConnection) {
                String friendlyName = ((BluetoothConnection) connection).getFriendlyName();
                setStatus(friendlyName, Color.MAGENTA);
                DemoSleeper.sleep(500);
            }
        } catch (ConnectionException e) {
            setStatus(e.getMessage(), Color.RED);
        }
    }
/*
    private void enableTestButtons(final boolean enabled) {
        runOnUiThread(new Runnable() {
            public void run() {
                printButton.setEnabled(true);
                printManyLabels.setEnabled(true);
            }
        });
    }
*/

    private byte[] getConfigLabel() {
        byte[] configLabel = null;
        try {
            PrinterLanguage printerLanguage = printer.getPrinterControlLanguage();
            SGD.SET("device.languages", "zpl", connection);


            if (printerLanguage == PrinterLanguage.ZPL) {

                //zpl code tested using http://labelary.com/viewer.html

                String bytes = "^XA\n" +
                        "\n" +
                        "^FX //Top section with logo, name and address.\n" +
                        "^CF0,40\n" +
                        "^FO100,40^FDConcept Engineering Consultants Ltd.^FS\n" +
                        "\n" +
                        "^FO30,95^GB800,1,3^FS //creates dividing line\n" +
                        "\n" +
                        "^FX //Second section with recipient address and permit information.\n" +
                        "^CFA,30\n" +
                        "^FO50,120^FDProject: "+projectNoAndName+"^FS\n" +
                        "^FO50,160^FDSample No.: "+sampleNo+"^FS\n" +
                        "^FO50,200^FDLocation: "+location+"^FS\n" +
                        "^FO50,240^FDSample Type: "+sampleType+"^FS\n" +
                        "^FO50,280^FDTop Depth (m): "+topDepth+"m^FS\n" +
                        "^FO50,320^FDBottom Depth (m): "+bottomDepth+"m^FS\n" +
                        "^FO50,360^FDSampling Date: "+samplingDate+"^FS\n" +
                        "\n" +
                        "^FO30,400^GB800,1,3^FS //creates dividing line\n" +
                        "\n" +
                        "^FX //Third section with QR Code.\n" +
                        "^FO620,400\n" +
                        "^BQN,2,4\n" +
                        "^FDMM,A"+qrCode+"\n" +
                        "^FS\n" +
                        "\n" +
                        "^XZ";
                configLabel = bytes.getBytes();

            } else if (printerLanguage == PrinterLanguage.CPCL) {
                String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "ON-FEED IGNORE\r\n" + "BOX 20 20 380 380 8\r\n" + "T 0 6 137 177 TEST\r\n" + "PRINT\r\n";
                configLabel = cpclConfigLabel.getBytes();
            }
        } catch (ConnectionException e) {
            Log.e("ConectionExeption",e.getMessage()+" "+e.getCause());
        }
        return configLabel;
    }

    private void doConnectionTest() {
        //printer = connect(); i greyed this out so that you dont have to connect to printer every time you print a label

        if (printer != null) {
            if (itemPosition>1){
                for (int j = 0; j <itemPosition; j++){
                    sendTestLabel();
                }
            }
            else{
                sendTestLabel();
            }
            //disconnect(); i greyed this out so you dont disconnect from printer after every time you print a label
        } else {
            disconnect();
        }
    }

    private void toggleEditField(EditText editText, boolean set) {
        editText.setEnabled(set);
        editText.setFocusable(set);
        editText.setFocusableInTouchMode(set);
    }
/*
    private boolean isBluetoothSelected() {
        return btRadioButton.isChecked();
    }
*/
    private String getMacAddressFieldText() {
        return macAddressEditText;
    }

    /*
    private String getTcpAddress() {
        return ipAddressEditText.getText().toString();
    }

    private String getTcpPortNumber() {
        return portNumberEditText.getText().toString();
    }
    */
}
