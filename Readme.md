# AWS Cleaner version 6.0.0

Note we are not responsible for any use of this application. Warranty is not expressed nor implied!  Use at your own
risk.

A AWS Java SDK based spring boot command line application that removes AWS account resources. Designed to be scheduled
on say on hourly basis after work to clean out development AWS accounts, etc.

Supports assume role, MFA interactive login or straight AWS credentials as per the AWS Java SDK.

Dry run by default, logs resources to be destroyed.
                 
Supports a white list on commandline for stacks to keep.

## Destroys:

* Cloudformation (in reverse dependency order by export)
* "Orphan" resources that do not exist in a cloudformation stack:
  * SNS
  * SQS
  * S3
  * Dynamodb
  * Elasticache
  * Elastic Beanstalk Applications

Framework for adding your own cleaners as spring beans.
          
## Usage:
Dry run by default.  Add --commit after the -jar To commit changes

```
java -D.... -jar aws-cleaner-6.0.0.jar 
-Dcleaner.region=<region> to override AWS region.
-Dcleaner.cloudformation.whitelist=<comma,separated,stack,name,prefixes> to keep named stacks.
-Dcleaner.role.arn=<roleArn> role to assume to access AWS.
-Dcleaner.mfa.arn=<mfaArn> device to use with Multi Factor Authentication (prompts for code).
```

## Minimum Requirements

* Java 17 (< version 6 is 11)
* Access to aws account with appropriate privileges to destroy resources (this may be by assume role).
                
## Binaries

### curl
```
curl -O https://repo1.maven.org/maven2/com/limemojito/oss/aws/aws-cleaner/6.0.0/aws-cleaner-6.0.0.jar
```


### Maven dependency

```
<dependency>
  <groupId>com.limemojito.oss.aws</groupId>
  <artifactId>aws-cleaner</artifactId>
  <version>6.0.0</version>
</dependency>
```


## Change log

### 6.0 (2024)

### 6.0.0
* Updated to latest open source framework and moved to GitHub.   Java 17 minimum required.

---

### 5.2 (2022-2023)

### 5.2.9
* s3 only cleans buckets from the region being cleaned.

#### 5.2.7
* Support regions without elastic beanstalk such as ap-southeast-4.

#### 5.2.6
* LogGroup cleaner - removes log groups where not in cloudformation and the storage has dropped to 0b
* SNS Cleaner - Updated to remove "dangling" SQS subscriptions when the Q no longer exists.

#### 5.2.5

* Update s3 cleaner to include object versions.  Library updates.

#### 5.2.4

* Remove stacks in Delete_failed status.

#### 5.2.3

* Library updates, boot version update

#### 5.2.2

* Library updates, boot version update

#### 5.2.1

* Exponential back off (7 attempts) on delete failure. I'm looking at you ECS Capacity Provider.
* Library updates, boot version update

#### 5.2.0

* SQS Queue Cleaner (v2).

---

### 5.1 (2020)

#### 5.1.3

* Library updates, boot version update

#### 5.1.2

* Library updates, boot version update

#### 5.1.1

* Loop retry on delete failure of ECS Capacity Provider.
* Library updates, boot version update

#### 5.1.0

* Cloudformation delete in reverse export order.
* Library updates, boot version update

---    

### 5.0 (2020)

#### 5.0.1

* Remove certificate resource cleaner as hits AWS cert issue limits faster with repeated use.

#### 5.0.0

* Certificate resource cleaner to remove certificates not in use.
* Library updates, boot version update, JDK update to 11

---    

### 4.0 (2018)

#### 4.0.2

* Remove Wait for stack delete. Can catchup on next run (assuming looped runs)

#### 4.0.1

* Remove region configuration.
* Assume role with MFA option
* Always execute CFM first, then clean orphan resources.

---    

### 3.0 (2018)

#### 3.0.4

* OSS release on apache 2.0 licence
* Cloudformation resource clearing
* SNS clearing including subscriptions
* S3 Resource clearing
* SQS resource clearing
* Elasticache clearing
* Elastic Beanstalk clearing
* Dynamo db table clearing
* Resource white list filter

### 2.0 - (2016)
     
### 1.0 - (2016)
