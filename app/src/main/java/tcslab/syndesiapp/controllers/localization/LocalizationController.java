package tcslab.syndesiapp.controllers.localization;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.ml.KNearest;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.io.*;
import java.util.*;

/**
 * Created by blais on 30.11.2016.
 */
public class LocalizationController {
    private static LocalizationController mInstance;
    private Context mAppContext;
    static final int READ_BLOCK_SIZE = 100;
    private File file;
    private String mCurrentPosition;
    private int nbReadings = 0;
    private Boolean toastPermission = false;


    public int numberAN;
    private List<double[]> samplesFloorList=new ArrayList<double[]>();
    private double[][] samplesFloor;
    private int numberSamples, numberAttributes;
    private KNearest knn;
    private SVM svm;
    private String fileName = "rssData.txt";
    private double[] mRSSIs;

    // Geneve AP MAC address
/*    private final static String[] mAnchorNodes={
            "2c:56:dc:d2:06:a8",
            "9c:5c:8e:c5:fb:a0",
            "9c:5c:8e:c5:f1:1a",
            "9c:5c:8e:c5:fb:7a",
            "9c:5c:8e:c5:fb:a6"
    };*/

    // Bussigny AP MAC address
    private final static String[] mAnchorNodes={
            "1c:87:2c:67:80:3c",
            "88:f7:c7:44:fb:40",
            "1c:87:2c:67:80:3c",
            "a4:52:6f:a5:45:11",
            "4e:66:41:fd:26:47"
    };


    private LocalizationController(Context mAppContext) {
        this.mAppContext = mAppContext;
        this.checkFile();
        mRSSIs = new double[mAnchorNodes.length];
        for(int i=0; i < mAnchorNodes.length; i++){
            mRSSIs[i] = 0;
        }
    }

    public static synchronized LocalizationController getInstance(Context appContext) {
        if (mInstance == null) {
            mInstance = new LocalizationController(appContext.getApplicationContext());
        }
        return mInstance;
    }

    public String updateLocation(List<List<ScanResult>> readings){
        HashMap<Double, Integer> results = new HashMap<>();
        ScanResult scanResult;

        for(List<ScanResult> APsList : readings) {
            for (int i = 0; i < APsList.size(); i++) {
                scanResult = APsList.get(i);

                //search by SSID or MAC
                for (int j = 0; j < mAnchorNodes.length; j++) {
                    if (scanResult.BSSID.equals(mAnchorNodes[j])) {
                        mRSSIs[j] = scanResult.level;
                    }
                }
            }

            double[] response = this.checkFloorSVN(mRSSIs);

            if(response.length == 1){
                return "-1.0";
            }else {
                for(double resp : response){
                    if (results.containsKey(resp)) {
                        results.put(resp, results.get(resp) + 1);
                    } else {
                        results.put(resp, 1);
                    }
                }
            }
        }

        double maxPosition = -1;
        String toastMessage = "";
        for(Double result : results.keySet()){
            toastMessage += result + " (" + results.get(result) + "), ";
            if(maxPosition == -1 || results.get(result) > results.get(maxPosition)){
                maxPosition = result;
            }
        }

        toastMessage = toastMessage.substring(0, toastMessage.length() - 2);
        this.toaster(toastMessage);
        Log.d("Loc", maxPosition + ": " + results.get(maxPosition));

        this.mCurrentPosition = Double.toString(maxPosition);

        return this.mCurrentPosition;
    }

    // Read text from file and train machine learning approaches
    public void readTraining() {
        try {
            // FileReader reader = new FileReader(file);
            BufferedReader breader = new BufferedReader(new FileReader(file));
            char[] inputBuffer = new char[READ_BLOCK_SIZE];
            String s = "";
            String[] features;
            try {
                while ((s = breader.readLine()) != null) {
                    features = s.split("\t");
                    if(features.length!=1) {
                        int length = features.length;

                        double[] featuresT = new double[length];
                        for (int column = 0; column < length; column++) {
                            featuresT[column] = Double.parseDouble(features[column]);
                        }
                        samplesFloorList.add(featuresT);
                    }

                }
                if(samplesFloorList.size() > 0) {
                    //Put samples in a double[][] array before training
                    samplesFloor = new double[samplesFloorList.size()][mAnchorNodes.length + 1];
                    int rowCount = 0;
                    for (double[] sample : samplesFloorList) {
                        samplesFloor[rowCount] = sample;
                        rowCount++;
                    }

                    //Once training samples are uploaded, train machine learning
                    numberAttributes = mAnchorNodes.length;
                    numberSamples = samplesFloor.length;
                    matTrainD = new Mat(numberSamples, numberAttributes, CvType.CV_32F);//Training set rows, cols, type
                    matTrainL = new Mat(numberSamples, 1, CvType.CV_32SC1); //the same number of rows
                    matTest = new Mat(1, numberAttributes, CvType.CV_32F);//Set to test
                    //Training data are int, labels are double
                    for (int i = 0; i < numberSamples; i++) {
                        for (int j = 0; j <= numberAttributes; j++) {
                            matTrainD.put(i, j, samplesFloor[i][j]);
                            if (j == numberAttributes) { //fill the label
                                matTrainL.put(i, 0, (int) samplesFloor[i][j]);
                            }
                        }
                    }

                    knn.train(matTrainD, Ml.ROW_SAMPLE, matTrainL);
                    svm.setType(SVM.C_SVC);
                    svm.setKernel(SVM.LINEAR);
                    svm.setGamma(3);
                    svm.train(matTrainD, Ml.ROW_SAMPLE, matTrainL);

                    Mat centers = new Mat();
                    TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
                    Core.kmeans(matTrainD, matTrainL.rows(), matTrainL, criteria, 1, Core.KMEANS_PP_CENTERS, centers);
                }
            }
            catch (Exception es)
            {
                Log.d("Error read samples:  ", es.getMessage());
            }
            breader.close();
        }
        catch (IOException e) {
            toaster("Error reading file: "+e.getMessage());
        }

    }


    //********Machine learning class*****//
    private Mat matTrainD, matTrainL, matTest, matResp;
    public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(mAppContext) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    toaster("OpenCV loaded successfully");
                    knn = KNearest.create();
                    svm = SVM.create();
                    matResp = new Mat(3,1,CvType.CV_32F);// row =number of neighbors
                    //Training process RSSI AND GEOMAGNETIC FIELD
                    if(file.exists()) {
                        readTraining();
                    }else{
                        toaster("Cannot train the classifiers: missing training file");
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public double[] checkFloorSVN(double[] test){
        if(file.exists()) {
            //Arrange  Mat to test RSS AND MAGNETIC FIELD
            for (int i = 0; i < numberAttributes; i++) {
                matTest.put(0, i, test[i]);
            }
            knn.findNearest(matTest, 3, matResp);
            int respS = (int) svm.predict(matTest);
            int respK = (int) matResp.get(0, 0)[0];
            return new double[]{respS, respK};  //return both response
        }
        return null;
    }

    public void checkFile(){
        file = new File(mAppContext.getExternalFilesDir(null), fileName);
        if(!file.exists()){
            toaster("No training file detected");
        }
    }

    public void toaster(String message)
    {
        if(this.toastPermission) {
            Toast.makeText(mAppContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    public String getmCurrentPosition() {
        return mCurrentPosition;
    }

    public Boolean getToastPermission() {
        return toastPermission;
    }

    public void setToastPermission(Boolean toastPermission) {
        this.toastPermission = toastPermission;
    }
}
