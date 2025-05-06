# Requirements

## Overview

1. **Registration** and **Authentication**:
   - Ability to register to the system.
   - Ability to login into the system.


2. **Friendship**:
    - Ability to send friend requests to other users.
    - Possibility of accepting or rejecting friend requests.
    - Notification system for receiving new friend requests.


3. **Messaging**:
    - Messaging management among friends.
    - Notification system for the arrival of new messages.


4. **Contents**:
    - Publish posts.
    - Visualize posts published by friends.
    - Possibility to filter friends' posts by a keyword.


5. **Administration**:
    - Ability to block users.
    - Ability to unblock users.
    - Metrics on system requests.


## User Stories

To define the system requirements and extract the ubiquitous language, user stories have been written.
These allow us to reason about the system from the perspective of the end user,
guiding the modeling and development process in line with the expectations of the client;
in this case, the future community and system administrators.

### Registration and Authentication

1. **User Registration**\
   As a **new user**,\
   I want to register to the system,\
   so that I can create an account and start using the application.


2. **User Login**\
   As a **registered user**,\
   I want to log into the system,\
   so that I can access my account and use the application's features.

### Friendship

1. **Send Friend Requests**\
   As a **user**,\
   I want to send friend requests to other users,\
   so that I can connect with them and add them to my friends list.


2. **Accept/Reject Friend Requests**\
   As a **user**,\
   I want to accept or reject friend requests,\
   so that I can manage my connections and decide who I want to be friends with.


3. **Friend Request Notifications**\
   As a **user**,\
   I want to receive notifications for new friend requests,\
   so that I am aware when someone wants to connect with me.

### Messaging

1. **Messaging Friends**\
   As a **user with friends**,\
   I want to send and receive messages with my friends,\
   so that we can communicate through the application.


2. **Message Notifications**\
   As a **user**,\
   I want to receive notifications for new messages,\
   so that I know when I have a new message from a friend.

### Contents

1. **Publish Contents**\
   As a **user with friends**,\
   I want to publish posts,\
   so that I can share my thoughts with my friends.


2. **View Contents**\
   As a **user with friends**,\
   I want to visualize posts published by my friends,\
   so that I can stay updated on what’s on my friends' minds.


3. **Search by Keyword**\
   As a **user**,\
   I want to be able to search through my friends' posts by entering a keyword,\
   so that I can see the posts that contain it.

### Administration

1. **Block and Unblock Users**\
   As an **administrator**,\
   I want to block and unblock users,\
   so that I can maintain the social network a safe place for all the users.


2. **Emergency Management**\
   As an **administrator**,\
   I want to receive a notification when something critical happens in the system,\
   so that I can prevent the system crash when users publish too much posts.

## Quality Attributes
Non-Functional Property (or Quality attributes) 
of a software system are constraint on the manner in which the system implements and delivers its functionality.

In our case, we have identified the following non-functional properties.

### Operational Properties
1. **Scalability**\
   Ability of horizontal scalability to handle increasing operational load.
2. **Observability**\
   Monitoring of system performance.
3. **Reliability**\
   System consistency.

### Structural Properties
1. **Extensibility**\
   Clearly defined architecture to allow the addition of new features.
2. **Maintainability**\
   Artifact modularity, well-structured and understandable source code.
3. **Testability**\
   - Automated testing.
   - Unit Testing.
   - Integration testing.

### Security Properties
1. Authentication of users to verify their identity.
2. Authorization of users to access resources according to established rules.
3. Encryption to ensure confidentiality of user passwords.

[« Back to Index](../docs.md) | [Next »](../design/event-storming.md)