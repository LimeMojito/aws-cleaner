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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.limemojito.aws.cleaner.Main;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:/cleaner.properties")
@ComponentScan(basePackageClasses = Main.class)
public class CleanerConfig {

    private final Regions defaultRegion;

    public CleanerConfig() {
        defaultRegion = Regions.US_WEST_2;
    }

    @Bean
    public AWSCredentialsProvider credentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    @Bean
    public AmazonIdentityManagement identityManagement(AWSCredentialsProvider credentialsProvider) {
        return AmazonIdentityManagementClient.builder()
                                             .withCredentials(credentialsProvider)
                                             .build();
    }

    @Bean
    public AmazonDynamoDB dynamoDBClient(AWSCredentialsProvider credentialsProvider) {
        return AmazonDynamoDBClient.builder()
                                   .withCredentials(credentialsProvider)
                                   .withRegion(defaultRegion)
                                   .build();
    }

    @Bean
    public AWSElasticBeanstalk ebClient(AWSCredentialsProvider credentialsProvider) {
        return AWSElasticBeanstalkClient.builder()
                                        .withCredentials(credentialsProvider)
                                        .withRegion(defaultRegion)
                                        .build();
    }

    @Bean
    public AmazonS3 s3Client(AWSCredentialsProvider credentialsProvider) {
        return AmazonS3Client.builder()
                             .withCredentials(credentialsProvider)
                             .withRegion(defaultRegion)
                             .build();
    }

    @Bean
    public AmazonSNS snsClient(AWSCredentialsProvider credentialsProvider) {
        return AmazonSNSClient.builder()
                              .withCredentials(credentialsProvider)
                              .withRegion(defaultRegion)
                              .build();
    }

    @Bean
    public AmazonElastiCache elastiCacheClient(AWSCredentialsProvider credentialsProvider) {
        return AmazonElastiCacheClient.builder()
                                      .withCredentials(credentialsProvider)
                                      .withRegion(defaultRegion)
                                      .build();
    }

    @Bean
    public AmazonCloudFormation cloudFormationClient(AWSCredentialsProvider credentialsProvider) {
        return AmazonCloudFormationClient.builder()
                                         .withCredentials(credentialsProvider)
                                         .withRegion(defaultRegion)
                                         .build();
    }
}
