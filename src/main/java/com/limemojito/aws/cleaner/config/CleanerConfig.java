/*
 * Copyright 2018 Lime Mojito Pty Ltd
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:/cleaner.properties")
@ComponentScan(basePackageClasses = Main.class)
public class CleanerConfig {

    @Bean
    public Regions region(@Value("${cleaner.region}") String regionName) {
        return Regions.fromName(regionName);
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
    public AmazonDynamoDB dynamoDBClient(AWSCredentialsProvider credentialsProvider, Regions defaultRegion) {
        return AmazonDynamoDBClient.builder()
                                   .withCredentials(credentialsProvider)
                                   .withRegion(defaultRegion)
                                   .build();
    }

    @Bean
    public AWSElasticBeanstalk ebClient(AWSCredentialsProvider credentialsProvider, Regions defaultRegion) {
        return AWSElasticBeanstalkClient.builder()
                                        .withCredentials(credentialsProvider)
                                        .withRegion(defaultRegion)
                                        .build();
    }

    @Bean
    public AmazonS3 s3Client(AWSCredentialsProvider credentialsProvider, Regions defaultRegion) {
        return AmazonS3Client.builder()
                             .withCredentials(credentialsProvider)
                             .withRegion(defaultRegion)
                             .build();
    }

    @Bean
    public AmazonSNS snsClient(AWSCredentialsProvider credentialsProvider, Regions defaultRegion) {
        return AmazonSNSClient.builder()
                              .withCredentials(credentialsProvider)
                              .withRegion(defaultRegion)
                              .build();
    }

    @Bean
    public AmazonElastiCache elastiCacheClient(AWSCredentialsProvider credentialsProvider, Regions defaultRegion) {
        return AmazonElastiCacheClient.builder()
                                      .withCredentials(credentialsProvider)
                                      .withRegion(defaultRegion)
                                      .build();
    }

    @Bean
    public AmazonCloudFormation cloudFormationClient(AWSCredentialsProvider credentialsProvider, Regions defaultRegion) {
        return AmazonCloudFormationClient.builder()
                                         .withCredentials(credentialsProvider)
                                         .withRegion(defaultRegion)
                                         .build();
    }
}
