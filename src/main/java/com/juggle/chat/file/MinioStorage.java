package com.juggle.chat.file;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;

public class MinioStorage {
//    private final MinioClient client;
//    private final String bucket;

    public MinioStorage(MinioConfig config) {
//        this.client = MinioClient.builder()
//                .endpoint(config.getEndpoint())
//                .credentials(config.getAccessKey(), config.getSecretKey())
//                .build();
//        this.bucket = config.getBucket();
    }

    public String preSignedPutUrl(String objectKey)
            throws ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
            ServerException, XmlParserException, IOException, NoSuchAlgorithmException, InvalidKeyException {
//        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
//                .bucket(bucket)
//                .object(objectKey)
//                .method(Method.PUT)
//                .expiry(15, TimeUnit.MINUTES)
//                .build();
//        return client.getPresignedObjectUrl(args);
        return null;
    }

}
