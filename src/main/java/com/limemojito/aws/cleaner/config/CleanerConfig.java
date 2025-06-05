/*
 * Copyright 2011-2025 Lime Mojito Pty Ltd
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

/**
 * Spring configuration class for the AWS resource cleaner application.
 * This class defines all the beans required for the application, including AWS clients
 * for various services, credential providers, and region configuration.
 */
@Configuration
@PropertySource("classpath:/cleaner.properties")
@ComponentScan(basePackageClasses = Main.class)
public class CleanerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanerConfig.class);

    /**
     * Provides the AWS region to use for all AWS clients.
     *
     * @param regionName The region name from configuration
     * @return The AWS region enum value
     */
    @Bean
    public Regions region(@Value("${cleaner.region}") String regionName) {
        return Regions.valueOf(regionName);
    }

    /**
     * Creates an AWS Security Token Service client.
     * This client is used for assuming roles and handling MFA authentication.
     *
     * @param region The AWS region to use
     * @return The AWS Security Token Service client
     */
    @Bean(destroyMethod = "shutdown")
    public AWSSecurityTokenService tokenService(Regions region) {
        return AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion(region)
                .build();
    }

    /**
     * Prompts for and returns an MFA code if MFA is configured.
     *
     * @param mfaArn The ARN of the MFA device, if any
     * @return The MFA code entered by the user, or an empty string if MFA is not configured
     */
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

    /**
     * Creates an AWS credentials provider that handles role assumption and MFA if configured.
     * If a role ARN is provided, assumes that role. If an MFA ARN is also provided,
     * uses the MFA code when assuming the role.
     *
     * @param roleArn The ARN of the role to assume, if any
     * @param mfaArn The ARN of the MFA device, if any
     * @param tokenService The AWS Security Token Service client
     * @param mfaCode The MFA code, if MFA is configured
     * @return An AWS credentials provider
     */
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

    /**
     * Creates an AWS Identity and Access Management (IAM) client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @return The AWS IAM client
     */
    @Bean
    public AmazonIdentityManagement identityManagement(AWSCredentialsProvider credentialsProvider) {
        return AmazonIdentityManagementClient.builder()
                .withCredentials(credentialsProvider)
                .build();
    }

    /**
     * Creates an AWS DynamoDB client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS DynamoDB client
     */
    @Bean
    public AmazonDynamoDB dynamoDBClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonDynamoDBClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS Elastic Beanstalk client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS Elastic Beanstalk client
     */
    @Bean
    public AWSElasticBeanstalk ebClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AWSElasticBeanstalkClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS S3 client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS S3 client
     */
    @Bean
    public AmazonS3 s3Client(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonS3Client.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS Simple Notification Service (SNS) client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS SNS client
     */
    @Bean
    public AmazonSNS snsClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonSNSClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS Simple Queue Service (SQS) client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS SQS client
     */
    @Bean
    public AmazonSQS sqsClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonSQSClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS ElastiCache client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS ElastiCache client
     */
    @Bean
    public AmazonElastiCache elastiCacheClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonElastiCacheClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS CloudFormation client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS CloudFormation client
     */
    @Bean
    public AmazonCloudFormation cloudFormationClient(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AmazonCloudFormationClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    /**
     * Creates an AWS Certificate Manager client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS Certificate Manager client
     */
    @Bean
    public AWSCertificateManager certificateManager(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AWSCertificateManagerClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region).build();
    }

    /**
     * Creates an AWS CloudWatch Logs client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region The AWS region to use
     * @return The AWS CloudWatch Logs client
     */
    @Bean
    public AWSLogs cloudWatch(AWSCredentialsProvider credentialsProvider, Regions region) {
        return AWSLogsClient.builder()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }
}
