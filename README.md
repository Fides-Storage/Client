Fides Storage
======
The goal of our project is to create a proof of concept secure cloud storage.
To guarantee the safety of the user's files, all files are encrypted on the client side. Because the files are encrypted locally, and the key is only accessible on the user's side, the user can safely put the files on an externally hosted server.

Fides Storage Client
======
The client side centers around a keyfile. This file contains a list of all the user's files, their location and their encryption keys. To prevent cryptoanalysis on the user's files, every file is encrypted with a different encryption key. These keys are stored with the file names in the user's keyfile. 

When the user adds, removes or updates files, the keyfile gets updated with the new files and their keys. The keyfile is then encrypted with the user's masterkey, which is derived from a password or a key file. After encrypting the keyfile, it is uploaded to the server.

To synchronise with the server, the client will first download and decrypt the keyfile that's saved on the server. In this file the client can see which files it's missing and which files it needs to upload.
