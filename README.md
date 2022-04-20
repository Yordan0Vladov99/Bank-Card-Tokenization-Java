# Bank-Card-Tokenization
A bank system that allows users to tokenize their credit cards.
Tokenization means transforming a 16-digit credit card number into a unique token.
A token must meet the following requirements:
  - the number of token digits and credit card digits must be equal
  - the first 12 digits of the token must be randomly generated and must not match the corresponding credit card digits
  - the first digit of the token must be equal to 3,4,5 or 6, because these digits are used by the major credit card companies
  - the sum of the token digits must be divisible by 10
 
The system implemets control access logic, where users fall into two groups:
1. Users that can register the token of a credit card. By giving the number of a valid credit card the user receives a unique token. The system checks for the card's validity, i.e if it starts with 3,4,5 or 6 and if it satisfies Luhn's formula and returns an error message when these conditions are not met.
2. Users that can extract a credit card number by giving a token number. The system returns an error if the token isn't valid or isn't registered.

The system is realized as multithreaded server-client app. 
The server implements the following functions:
  - stores the card token pairs inside a XML file
  - stores the user information (user, password, registration rights, extraction rights) inside a XML file
  - validates clients by accepting a valid username and password and returnig the user's rights (whether thay can register, extract or both)
  - handles regitsration request by accepting a valid credit, generating and returning a valid unique token
  - handles extraction requests by accepting a valid registered token and returning the credit card with which the token was registered.
  - writes to text file all the card-token pairs, sorted by the credit card number
  - writes to text file all the card-token pairs, sorted by the token number
  
 ![pic-3](https://user-images.githubusercontent.com/43996329/156048173-73a4dabd-e792-4bb7-88e8-ed8e50e45147.png)

 

The client connects to server by providing a valid user name and password.
![pic-7](https://user-images.githubusercontent.com/43996329/156048300-57e7265f-de87-42f8-a1ab-64d75499e2d1.png)

The system returns an error if the user submits an invalid user name or password.
![pic-6](https://user-images.githubusercontent.com/43996329/156048528-a77ae0a9-4d34-4ed8-8bca-b70650ab917f.png)

The system also returns an error if the submitted username doesn't exist or if the password is incorrect.

![pic-5](https://user-images.githubusercontent.com/43996329/156048719-2d31888d-59e4-4da5-9880-dea839c9a990.png)

After connecting, the client interface updates depending on the client rights.
The client can then submit registration/extraction requests to server and visualize server responses.
In the following example the client registers a credit card and receives a token.

![pic-4](https://user-images.githubusercontent.com/43996329/156049012-f6ec0aed-06de-4e7c-9cc6-6ecdffa7b486.png)

After that the user can extract the credit card using the same token.

![pic-2](https://user-images.githubusercontent.com/43996329/156049102-e0d55ee3-ddd4-4112-b2e6-c033c0c8e794.png)



