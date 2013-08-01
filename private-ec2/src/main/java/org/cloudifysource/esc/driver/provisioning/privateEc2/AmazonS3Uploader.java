/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.privateEc2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.packaging.ZipUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;

import de.idyl.winzipaes.AesZipFileEncrypter;
import de.idyl.winzipaes.impl.AESEncrypterBC;

/**
 * Class to help uploading file to Amazon S3.
 * 
 * @author victor
 * 
 */
public class AmazonS3Uploader {
	private final Logger logger = Logger.getLogger(AmazonS3Uploader.class.getName());

	private static final int FILE_PART_SIZE = 5242880;
	private AmazonS3 s3client;
	private String accessKey;

	public AmazonS3Uploader(final String accessKey, final String secretKey) {
		this(accessKey, secretKey, null);
	}

	public AmazonS3Uploader(final String accessKey, final String secretKey, final String locationId) {
		this.accessKey = accessKey;
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		this.s3client = new AmazonS3Client(credentials);
		if (locationId != null) {
			this.s3client.setRegion(RegionUtils.convertLocationId2Region(locationId));
		}
	}

	/**
	 * Zip and upload a folder.
	 * 
	 * @param existingBucketName
	 *            The name of the bucket where to download the file.
	 * @param pathFolderToZip
	 *            The folder to upload.
	 * @param archivePassword
	 *            The password for the archive.
	 * @return The URL to access the file in s3
	 * @exception IOException
	 *                When the zipping fails.
	 */
	public String zipAndUploadToS3(final String existingBucketName, final String pathFolderToZip,
			final String archivePassword) throws IOException {
		if (archivePassword == null) {
			throw new IllegalArgumentException("Password must be provided for cloud directory archive.");
		}
		File zipFile = this.zipFolder(pathFolderToZip, archivePassword);
		String s3Url = this.uploadFile(existingBucketName, zipFile);
		return s3Url;
	}

	File zipFolder(final String pathFolderToZip, final String archivePassword) throws IOException {

		final File zipFile = File.createTempFile("cloudFolder", ".zip");
		zipFile.deleteOnExit();
		ZipUtils.zip(new File(pathFolderToZip), zipFile);

		AesZipFileEncrypter zipEncrypter = null;
		File cryptedZipFile = null;
		try {
			cryptedZipFile = File.createTempFile("cloudFolderCrypted", ".zip");
			cryptedZipFile.deleteOnExit();
			logger.info("Cloud folder zip:" + cryptedZipFile.getCanonicalPath());

			AESEncrypterBC encrypter = new AESEncrypterBC();
			encrypter.init(archivePassword, 0); // The 0 is keySize, it is ignored for AESEncrypterBC
			zipEncrypter = new AesZipFileEncrypter(cryptedZipFile, encrypter);
			zipEncrypter.addAll(zipFile, archivePassword);
		} finally {
			if (zipEncrypter != null) {
				zipEncrypter.close();
			}
		}
		return cryptedZipFile;
	}

	/**
	 * Upload file.
	 * 
	 * @param existingBucketName
	 *            The name of the bucket where to download the file.
	 * @param file
	 *            The file to upload.
	 * @return The URL to access the file in s3
	 */
	public String uploadFile(final String existingBucketName, final File file) {
		PutObjectRequest putObjectRequest = new PutObjectRequest(existingBucketName, this.accessKey, file);
		putObjectRequest.setKey(file.getName());
		putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
		this.s3client.putObject(putObjectRequest);
		S3Object object = this.s3client.getObject(existingBucketName, file.getName());
		return object.getObjectContent().getHttpRequest().getURI().toString();

	}

	/**
	 * Upload file using multipart.
	 * 
	 * @param existingBucketName
	 *            The name of the bucket where to download the file.
	 * @param filePath
	 *            The path to the file to upload.
	 */
	public void uploadFileMultipart(final String existingBucketName, final String filePath) {

		// Create a list of UploadPartResponse objects. You get one of these
		// for each part upload.
		List<PartETag> partETags = new ArrayList<PartETag>();

		// Step 1: Initialize.
		InitiateMultipartUploadRequest initRequest =
				new InitiateMultipartUploadRequest(existingBucketName, this.accessKey);
		InitiateMultipartUploadResult initResponse = this.s3client.initiateMultipartUpload(initRequest);

		File file = new File(filePath);
		long contentLength = file.length();
		long partSize = FILE_PART_SIZE; // Set part size to 5 MB.

		try {
			// Step 2: Upload parts.
			long filePosition = 0;
			for (int i = 1; filePosition < contentLength; i++) {
				// Last part can be less than 5 MB. Adjust part size.
				partSize = Math.min(partSize, (contentLength - filePosition));

				// Create request to upload a part.
				UploadPartRequest uploadRequest = new UploadPartRequest()
						.withBucketName(existingBucketName).withKey(this.accessKey)
						.withUploadId(initResponse.getUploadId()).withPartNumber(i)
						.withFileOffset(filePosition)
						.withFile(file)
						.withPartSize(partSize);

				// Upload part and add response to our list.
				partETags.add(
						this.s3client.uploadPart(uploadRequest).getPartETag());

				filePosition += partSize;
			}

			// Step 3: complete.
			CompleteMultipartUploadRequest compRequest = new
					CompleteMultipartUploadRequest(
							existingBucketName,
							this.accessKey,
							initResponse.getUploadId(),
							partETags);

			this.s3client.completeMultipartUpload(compRequest);
		} catch (Exception e) {
			this.s3client.abortMultipartUpload(
					new AbortMultipartUploadRequest(existingBucketName, this.accessKey, initResponse.getUploadId()));
		}
	}

}
