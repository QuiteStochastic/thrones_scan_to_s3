package thrones_scan_to_s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.springframework.web.client.RestTemplate;

import java.io.*;

/**
 * Created by oliverl1
 */
public class ScanToS3 {


    private static final int numCharacters=42;
    private static final int numOrganizations=15;
    private static final int numLocations=32;
    private static final int numEvents=10;
    private static final int numEpisodes=40;

    private static final String siteDomain ="http://localhost/";


    private static final String bucketName = "valar-morghulis.org";

    private static final String bucketPolicy = "{\n" +
            "\t\"Version\": \"2008-10-17\",\n" +
            "\t\"Statement\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"Sid\": \"AllowPublicRead\",\n" +
            "\t\t\t\"Effect\": \"Allow\",\n" +
            "\t\t\t\"Principal\": {\n" +
            "\t\t\t\t\"AWS\": \"*\"\n" +
            "\t\t\t},\n" +
            "\t\t\t\"Action\": \"s3:GetObject\",\n" +
            "\t\t\t\"Resource\": \"arn:aws:s3:::"+bucketName+"/*\"\n" +
            "\t\t}\n" +
            "\t]\n" +
            "}";

    private static final String errorHtml="<html>\n" +
            "<body>\n" +
            "    <p>Error 404</p>\n" +
            "<body/>\n" +
            "<html/>";

    //private static AmazonS3 s3client;


    //test main
/*    public static void main(String[] args) {

        String test=simpleGet(siteDomain);
        //System.out.println(test);


        final AmazonS3 s3client;
        try {
            s3client = initS3Client();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("failed to create s3 client");
            return;
        }

        System.out.println("S3 CLIENT CREATED");

        if(!s3client.doesBucketExist(bucketName)){
            System.out.println("NO BUCKET, CREATING");
            s3client.createBucket(bucketName);
        }
        else{
            System.out.println("BUCKET ALREADY EXISTS");
        }
        s3client.setBucketAcl(new SetBucketAclRequest(bucketName,CannedAccessControlList.PublicRead));
        s3client.setBucketPolicy(bucketName,bucketPolicy);
        System.out.println("BUCKET OK");




        uploadStringToS3(s3client,"test.html",test,"text/html");



		TransferManager tm = new TransferManager(s3client);

		tm.uploadDirectory(bucketName, "", new File("/code/static/"), true);

    }*/


    public static void main(String[] args) {

        final AmazonS3 s3client;
        try {
            s3client = initS3Client();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("failed to create s3 client");
            return;
        }
        System.out.println("S3 CLIENT CREATED");


        if(!s3client.doesBucketExist(bucketName)){
            System.out.println("NO BUCKET, CREATING");
            s3client.createBucket(bucketName);
        }
        else{
            System.out.println("BUCKET ALREADY EXISTS");
        }
        s3client.setBucketAcl(new SetBucketAclRequest(bucketName,CannedAccessControlList.PublicRead));
        s3client.setBucketPolicy(bucketName,bucketPolicy);
        System.out.println("BUCKET OK");


        uploadStringToS3(s3client,"main",simpleGet(siteDomain),"text/html");
        uploadStringToS3(s3client,"about",simpleGet(siteDomain+"/about"),"text/html");
        uploadStringToS3(s3client,"error",errorHtml,"text/html");



        new Thread(() -> fetchPageAndUpload(s3client,"characters",numCharacters,"text/html")).start();
        new Thread(() -> fetchPageAndUpload(s3client,"organizations",numOrganizations,"text/html")).start();
        new Thread(() -> fetchPageAndUpload(s3client,"locations",numLocations,"text/html")).start();
        new Thread(() -> fetchPageAndUpload(s3client,"events",numEvents,"text/html")).start();
        new Thread(() -> fetchPageAndUpload(s3client,"episodes",numEpisodes,"text/html")).start();



		TransferManager tm = new TransferManager(s3client);
		tm.uploadDirectory(bucketName, "", new File("../thrones_db_spring/src/main/resources/static/"), true);

    }



    private static AmazonS3 initS3Client() throws Exception{
        String accessKeyId;
        String secretKeyId;

        try {


            BufferedReader br = new BufferedReader(new FileReader("./dev_user.txt"));



            //skip the first line
            br.readLine();

            String[] line=br.readLine().split(",");
            System.out.println("line: "+line[0]);
            //in my current file format, the needed fields are the 2nd and 3rd entry in the csv file
            accessKeyId=line[1];
            secretKeyId=line[2];


        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("failed to read cred strings");
            //return null;
            throw new Exception();
        }


        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretKeyId);
        AmazonS3 s3client= new AmazonS3Client(awsCreds);
        s3client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1));
        return s3client;

    }




    private static void fetchPageAndUpload(AmazonS3 s3client, String category,int numPages,String contentType) {

		uploadStringToS3(s3client,category,simpleGet(siteDomain+"/"+category),contentType);


		for(int n=1;n<=numPages;n++){

            final String keyName=category+"/"+ n;

            new Thread(()-> uploadStringToS3(s3client,keyName,simpleGet(siteDomain+"/"+keyName),contentType)).start();

        }

    }


    private static String simpleGet(String url){

        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.getForObject(url,String.class);
    }



	private static void uploadStringToS3(AmazonS3 s3client, String keyName, String uploadObject, String contentType){

		System.out.println("Uploading to S3\n");


		try {

			//if it exists, need to delete the old one
			if(s3client.doesObjectExist(bucketName,keyName)){
				s3client.deleteObject(new DeleteObjectRequest(bucketName,keyName));
				System.out.println("old file existed, now deleted\n");
			}


			//upload new object

			ByteArrayInputStream object = new ByteArrayInputStream(uploadObject.getBytes());

			ObjectMetadata metadata= new ObjectMetadata();
			metadata.setContentType(contentType);

			PutObjectRequest request= new PutObjectRequest(bucketName, keyName, object,metadata);
			//request.setCannedAcl(CannedAccessControlList.PublicRead);

			s3client.putObject(request);



        }
        catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        }
        catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }

		System.out.println("new object uploaded to S3\n");

	}




}
