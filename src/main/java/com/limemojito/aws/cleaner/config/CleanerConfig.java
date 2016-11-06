/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.limemojito.aws.cleaner.Main;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ComponentScan(basePackageClasses = Main.class)
public class CleanerConfig {

    private final Region defaultRegion;

    public CleanerConfig() throws IOException {
        defaultRegion = Region.getRegion(Regions.US_WEST_2);
    }

    @Bean
    public AWSCredentialsProvider credentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    @Bean
    public AmazonIdentityManagement identityManagement(AWSCredentialsProvider credentialsProvider) {
        return new AmazonIdentityManagementClient(credentialsProvider);
    }

    @Bean
    public AmazonDynamoDBClient dynamoDBClient(AWSCredentialsProvider credentialsProvider) {
        final AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDBClient.setRegion(defaultRegion);
        return amazonDynamoDBClient;
    }

    @Bean
    public AWSElasticBeanstalkClient ebClient(AWSCredentialsProvider credentialsProvider) {
        final AWSElasticBeanstalkClient awsElasticBeanstalkClient = new AWSElasticBeanstalkClient(credentialsProvider);
        awsElasticBeanstalkClient.setRegion(defaultRegion);
        return awsElasticBeanstalkClient;
    }

    @Bean
    public AmazonS3Client s3Client(AWSCredentialsProvider credentialsProvider) {
        final AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);
        s3Client.setRegion(defaultRegion);
        return s3Client;
    }

    @Bean
    public AmazonSNSClient snsClient(AWSCredentialsProvider credentialsProvider) {
        final AmazonSNSClient snsClient = new AmazonSNSClient(credentialsProvider);
        snsClient.setRegion(defaultRegion);
        return snsClient;
    }

    @Bean
    public AmazonElastiCacheClient elastiCacheClient(AWSCredentialsProvider credentialsProvider) {
        final AmazonElastiCacheClient elastiCacheClient = new AmazonElastiCacheClient(credentialsProvider);
        elastiCacheClient.setRegion(defaultRegion);
        return elastiCacheClient;
    }
}
