# Change log

# 8

### 8.0.11
* Updated versions and security patches.

### 8.0.10
* Updated versions and security patches.

### 8.0.9
* Updated versions and security patches.

### 8.0.8
* Updated versions and security patches.

### 8.0.7
* Updated versions and security patches.

### 8.0.6
* Updated versions and security patches.

### 8.0.5
* Updated versions and security patches.

### 8.0.4
* Updated versions and security patches.
* Build update.

### 8.0.0
* Spring Boot 4 and Java 25
             
# 7

### 7.1.2
* Spring version and Library Vulnerability Updates
* Correction to AWS elastic beanstalk error handling that now throws service level exceptions when EB is not available in a region.

### 7.1.1
* Spring version and Library Vulnerability Updates

### 7.1.0
* Added name based exclusion of resource deletion to help with CDK and ad-hoc manual setups such as once of event bridge, etc.
* Removed use of AWS SDk 1 due to end of life Dec 2025.

### 7.0.0 (2025)
* Java 21 required, updated to the latest OSS framework.
  
---
# 6

# 6 (2024)

### 6.0.0  (2024)
* Updated to latest open source framework and moved to GitHub.   Java 17 minimum required.

---
# 5

## 5.2 (2022-2023)

### 5.2.9
* s3 only cleans buckets from the region being cleaned.

### 5.2.7
* Support regions without elastic beanstalk such as ap-southeast-4.

### 5.2.6
* LogGroup cleaner - removes log groups where not in cloudformation and the storage has dropped to 0b
* SNS Cleaner - Updated to remove "dangling" SQS subscriptions when the Q no longer exists.

### 5.2.5

* Update s3 cleaner to include object versions.  Library updates.

### 5.2.4

* Remove stacks in Delete_failed status.

### 5.2.3

* Library updates, boot version update

### 5.2.2

* Library updates, boot version update

### 5.2.1

* Exponential back off (7 attempts) on delete failure. I'm looking at you ECS Capacity Provider.
* Library updates, boot version update

### 5.2.0

* SQS Queue Cleaner (v2).

---
## 5.1 (2020)

### 5.1.3

* Library updates, boot version update

### 5.1.2

* Library updates, boot version update

### 5.1.1

* Loop retry on delete failure of ECS Capacity Provider.
* Library updates, boot version update

### 5.1.0

* Cloudformation delete in reverse export order.
* Library updates, boot version update

---
### 5.0.1

* Remove certificate resource cleaner as hits AWS cert issue limits faster with repeated use.

### 5.0.0

* Certificate resource cleaner to remove certificates not in use.
* Library updates, boot version update, JDK update to 11

---    
# 4 (2018)

### 4.0.2

* Remove Wait for stack delete. Can catchup on next run (assuming looped runs)

### 4.0.1

* Remove region configuration.
* Assume role with MFA option
* Always execute CFM first, then clean orphan resources.

---    
# 3 (2018)

### 3.0.4

* OSS release on apache 2.0 licence
* Cloudformation resource clearing
* SNS clearing including subscriptions
* S3 Resource clearing
* SQS resource clearing
* Elasticache clearing
* Elastic Beanstalk clearing
* Dynamo db table clearing
* Resource white list filter

# 2 - (2016)
     
# 1 - (2016)
   
---
