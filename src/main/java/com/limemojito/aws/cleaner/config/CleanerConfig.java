/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
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

    private final Region region;
    private final AWSCredentials awsCredentials;

    public CleanerConfig() throws IOException {
        awsCredentials = new PropertiesCredentials(getClass().getResourceAsStream("/aws-credentials.properties"));
        region = Region.getRegion(Regions.US_WEST_2);
    }

    @Bean
    public AmazonDynamoDBClient dynamoDBClient() {
        final AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials);
        amazonDynamoDBClient.setRegion(region);
        return amazonDynamoDBClient;
    }

    @Bean
    public AWSElasticBeanstalkClient ebClient() {
        final AWSElasticBeanstalkClient awsElasticBeanstalkClient = new AWSElasticBeanstalkClient(awsCredentials);
        awsElasticBeanstalkClient.setRegion(region);
        return awsElasticBeanstalkClient;
    }

    @Bean
    public AmazonS3Client s3Client() {
        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
        s3Client.setRegion(region);
        return s3Client;
    }

    @Bean
    public AmazonSNSClient snsClient() {
        final AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentials);
        snsClient.setRegion(region);
        return snsClient;
    }

    @Bean
    public AmazonElastiCacheClient elastiCacheClient() {
        final AmazonElastiCacheClient elastiCacheClient = new AmazonElastiCacheClient(awsCredentials);
        elastiCacheClient.setRegion(region);
        return elastiCacheClient;
    }
}
