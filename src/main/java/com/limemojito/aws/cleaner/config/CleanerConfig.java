/*
 * Copyright 2011-2024 Lime Mojito Pty Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.limemojito.aws.cleaner.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.certificatemanager.AWSCertificateManager;
import com.amazonaws.services.certificatemanager.AWSCertificateManagerClientBuilder;
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
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.limemojito.aws.cleaner.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Scanner;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
@PropertySource("classpath:/cleaner.properties")
@ComponentScan(basePackageClasses = Main.class)
public class CleanerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanerConfig.class);

    @Bean
    public Regions region(@Value("${cleaner.region}") String regionName) {
        return Regions.fromName(regionName);
    }

    @Bean(destroyMethod = "shutdown")
    public AWSSecurityTokenService tokenService(Regions region) {
        return AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion(region)
                .build();
    }

    @Bean
    public String mfaCode(@Value("${cleaner.mfa.arn}") String mfaArn) {
        if (!isBlank(mfaArn)) {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Enter MFA code: ");
                return scanner.next();
            }
        }
        return "";
    }

    @Bean
    public AWSCredentialsProvider credentialsProvider(@Value("${cleaner.role.arn}") String roleArn,
                                                      @Value("${cleaner.mfa.arn}") String mfaArn,
                                                      AWSSecurityTokenService tokenService,
                                                      String mfaCode) {
        if (!isBlank(roleArn)) {
            LOGGER.info("Preparing credentials for Role: {}", roleArn);
            final AssumeRoleRequest roleRequest = new AssumeRoleRequest().withRoleArn(roleArn)
                    .withRoleSessionName("aws-cleaner");
            if (!isBlank(mfaArn)) {
                LOGGER.info("Using MFA code with {}", mfaArn);
                roleRequest.withSerialNumber(mfaArn);
                roleRequest.withTokenCode(mfaCode);
            }
            final Credentials credentials = tokenService.assumeRole(roleRequest)
                    .getCredentials();
            final BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                    credentials.getAccessKeyId(),
                    credentials.getSecretAccessKey(),
                    credentials.getSessionToken());
            return new AWSStaticCredentialsProvider(sessionCredentials);
        } else {
            return new DefaultAWSCredentialsProviderChain();
        }
    }

    @Bean
    public AmazonIdentityManagement identityManagement(AWSCredentialsProvider credentialsProvider) {
        return AmazonIdentityManagementClient.builder()
                .withCredentials(credentialsProvider)
                .build();
    }

    @Bean
    public AmazonDynamoDB dynamoDBClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonDynamoDBClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AWSElasticBeanstalk ebClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AWSElasticBeanstalkClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AmazonS3 s3Client(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonS3Client.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AmazonSNS snsClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonSNSClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AmazonSQS sqsClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonSQSClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AmazonElastiCache elastiCacheClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonElastiCacheClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AmazonCloudFormation cloudFormationClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonCloudFormationClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public AWSCertificateManager certificateManager(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AWSCertificateManagerClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region).build();
    }

    @Bean
    public AWSLogs cloudWatch(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AWSLogsClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }
}
