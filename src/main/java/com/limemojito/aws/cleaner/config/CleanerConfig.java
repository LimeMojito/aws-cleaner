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


import com.limemojito.aws.cleaner.Main;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

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
@Slf4j
public class CleanerConfig {

    /**
     * Provides the AWS region to use for all AWS clients.
     *
     * @param regionName The region name from configuration
     * @return The AWS region enum value
     */
    @Bean
    public Region region(@Value("${cleaner.region}") String regionName) {
        return Region.of(regionName);
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
     * Creates an AWS Security Token Service client.
     * This client is used for assuming roles and handling MFA authentication.
     *
     * @param region The AWS region to use
     * @return The AWS Security Token Service client
     */
    @Bean(destroyMethod = "close")
    public StsClient tokenService(Region region) {
        return StsClient.builder()
                        .region(region)
                        .build();
    }

    /**
     * Creates an AWS credentials provider that handles role assumption and MFA if configured.
     * If a role ARN is provided, assumes that role. If an MFA ARN is also provided,
     * uses the MFA code when assuming the role.
     *
     * @param roleArn      The ARN of the role to assume, if any
     * @param mfaArn       The ARN of the MFA device, if any
     * @param tokenService The AWS Security Token Service client
     * @param mfaCode      The MFA code, if MFA is configured
     * @return An AWS credentials provider
     */
    @Bean
    public AwsCredentialsProvider credentialsProvider(@Value("${cleaner.role.arn}") String roleArn,
                                                      @Value("${cleaner.mfa.arn}") String mfaArn,
                                                      StsClient tokenService,
                                                      String mfaCode) {
        if (!isBlank(roleArn)) {
            log.info("Preparing credentials for Role: {}", roleArn);
            final AssumeRoleRequest.Builder roleRequest = AssumeRoleRequest.builder()
                                                                           .roleArn(roleArn)
                                                                           .roleSessionName("aws-cleaner");
            if (!isBlank(mfaArn)) {
                log.info("Using MFA code with {}", mfaArn);
                roleRequest.serialNumber(mfaArn);
                roleRequest.tokenCode(mfaCode);
            }
            final Credentials credentials = tokenService.assumeRole(roleRequest.build()).credentials();
            final AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(credentials.accessKeyId(),
                                                                                          credentials.secretAccessKey(),
                                                                                          credentials.sessionToken());
            return StaticCredentialsProvider.create(sessionCredentials);
        } else {
            return DefaultCredentialsProvider.builder().build();
        }
    }

    /**
     * Creates an AWS Identity and Access Management (IAM) client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @return The AWS IAM client
     */
    @Bean(destroyMethod = "close")
    public IamClient identityManagement(AwsCredentialsProvider credentialsProvider) {
        return IamClient.builder()
                        .credentialsProvider(credentialsProvider)
                        .build();
    }

    /**
     * Creates an AWS DynamoDB client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS DynamoDB client
     */
    @Bean(destroyMethod = "close")
    public DynamoDbClient dynamoDBClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return DynamoDbClient.builder()
                             .credentialsProvider(credentialsProvider)
                             .region(region)
                             .build();
    }

    /**
     * Creates an AWS Elastic Beanstalk client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS Elastic Beanstalk client
     */
    @Bean(destroyMethod = "close")
    public ElasticBeanstalkClient ebClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return ElasticBeanstalkClient.builder()
                                     .credentialsProvider(credentialsProvider)
                                     .region(region)
                                     .build();
    }

    /**
     * Creates an AWS S3 client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS S3 client
     */
    @Bean(destroyMethod = "close")
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider, Region region) {
        return S3Client.builder()
                       .credentialsProvider(credentialsProvider)
                       .region(region)
                       .build();
    }

    /**
     * Creates an AWS Simple Notification Service (SNS) client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS SNS client
     */
    @Bean(destroyMethod = "close")
    public SnsClient snsClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return SnsClient.builder()
                        .credentialsProvider(credentialsProvider)
                        .region(region)
                        .build();
    }

    /**
     * Creates an AWS Simple Queue Service (SQS) client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS SQS client
     */
    @Bean(destroyMethod = "close")
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return SqsClient.builder()
                        .credentialsProvider(credentialsProvider)
                        .region(region)
                        .build();
    }

    /**
     * Creates an AWS ElastiCache client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS ElastiCache client
     */
    @Bean(destroyMethod = "close")
    public ElastiCacheClient elastiCacheClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return ElastiCacheClient.builder()
                                .credentialsProvider(credentialsProvider)
                                .region(region)
                                .build();
    }

    /**
     * Creates an AWS CloudFormation client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS CloudFormation client
     */
    @Bean(destroyMethod = "close")
    public CloudFormationClient cloudFormationClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return CloudFormationClient.builder()
                                   .credentialsProvider(credentialsProvider)
                                   .region(region)
                                   .build();
    }

    /**
     * Creates an AWS CloudWatch Logs client.
     *
     * @param credentialsProvider The AWS credentials provider
     * @param region              The AWS region to use
     * @return The AWS CloudWatch Logs client
     */
    @Bean(destroyMethod = "close")
    public CloudWatchLogsClient cloudWatch(AwsCredentialsProvider credentialsProvider, Region region) {
        return CloudWatchLogsClient.builder()
                                   .credentialsProvider(credentialsProvider)
                                   .region(region)
                                   .build();
    }
}
