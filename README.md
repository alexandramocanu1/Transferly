# Transferly download

[https://github.com/alexandramocanu1/Transferly/releases/tag/v1.0](https://github.com/alexandramocanu1/Transferly/releases/tag/v2.0)


# Transferly

Secure file transfer and sharing platform built as a **Bachelor’s Thesis project** at the Faculty of Mathematics and Computer Science, University of Bucharest.

Transferly is an end-to-end system that allows users to upload files from an Android device, share them via unique public links, and collaborate through protected shared folders, with a strong focus on **security, data integrity, and access control**.

---

## Features

- Secure user authentication and registration (JWT + BCrypt)
- Upload images from Android devices
- Generate **unique public links** for sharing images
- Shared folders with controlled access between friends
- **AES encryption** for shared folder content
- **Shamir Secret Sharing** for distributed encryption key management
- NAS-based storage for uploaded files
- Responsive **web gallery** for public image viewing (no authentication required)

---

## Architecture Overview

Transferly follows a **client–server architecture**:

- **Android Client** – handles user interaction, file selection, upload, and sharing
- **Backend API (Spring Boot)** – authentication, authorization, business logic
- **Database (Oracle)** – persistence for users, friendships, folders, and access control
- **NAS Storage** – centralized storage for uploaded files
- **Web Gallery** – public visualization of shared images via unique links

---

## Tech Stack

### Mobile
- Android (Java)
- RecyclerView, Glide
- REST communication (HTTP multipart uploads)

### Backend
- Java, Spring Boot
- Spring Security
- JWT-based authentication
- Hibernate ORM
- Oracle Database

### Security
- BCrypt password hashing
- AES (256-bit) encryption for shared folders
- Shamir Secret Sharing for key distribution

### Web
- HTML5
- CSS3
- JavaScript

---

## Security Design

- Passwords are never stored in plain text; BCrypt hashing with salt is used
- Authentication is stateless using JWT tokens
- Public links are generated using **UUID v4**, making them non-guessable
- Shared folders are encrypted using AES
- Encryption keys are split across users using Shamir Secret Sharing, requiring a minimum threshold to reconstruct the key

---

## Database Design

The relational database includes the following core entities:

- Users
- Friendships (Many-to-Many)
- Shared Folders
- Folder Access Requests
- Encrypted Key Shares

The schema is designed to ensure data consistency, security, and scalability.

---

## Motivation

The project was developed to address the lack of simple yet secure file-sharing solutions that combine:

- Temporary public sharing via links
- Private, access-controlled collaboration
- Full control over data storage (no third-party cloud dependency)

---

## Future Improvements

- Support for additional file types (documents, video, audio)
- Expiring public links
- Push notifications for social interactions
- Cloud backup and synchronization
- Performance optimizations for large uploads

---

## Author

**Alexandra Mocanu**  
Bachelor of Computer Science  
Faculty of Mathematics and Computer Science, University of Bucharest

---

## License

This project was developed for academic purposes as part of a Bachelor’s thesis.
