##How to configure Parity's health monitor.

#### 1 Configure SMTP to send an email in case there is a problem with Parity

**Install mailutils**

```
sudo apt-get install mailutils
```


When the setup wizard launches, choose the unconfigured option. You don’t need to do any special configuration to get this to work.

**Install and configure sstmp**

```
sudo apt-get install ssmtp
sudo vim /etc/ssmtp/ssmtp.conf
```

Hit “i” to enter Insert mode.
Uncomment FromLineOverride=YES by deleting the #
Add the following to the file:
 
```
AuthUser=<user>@gmail.com
AuthPass=Your-Gmail-Password
mailhub=smtp.gmail.com:587
UseSTARTTLS=YES
```

Save and close the file:
Hit Escape
Type :wq
Hit Enter

**If you’re using two-factor authentication**
Create a new application-specific password to use in the config file above. (If you’re using Gmail, you can manage those passwords [here](https://www.google.com/accounts/IssuedAuthSubTokens).)

**Test it out**

```
echo "This is a test" | mail -s "Test" <user>@<email>.com
```


Using a webmail service other than Gmail
You can follow the same pattern that I used above. You’ll need to:

Subsitute the SMTP address and port for your email service (e.g. Yahoo!) where it says smtp.gmail.com:587. (587 is the port number.)
Set up an application-specific password if your webmail provider allows it, and paste that into the password line, the way I did with Gmail. (Yahoo! appears to have something similar.)

#### 2 Add /web/healthCheck.sh script to cron to automate script execution

**Test the script**

```
Usage:  ./healthCHeck.sh mail parityAddress
Sample: ./healthCHeck.sh mail@mail.com http://localhost:8547
```

**Add to cron**

```
sudo vim /etc/crontab
```

**Add the command with parameters**

This example would execute it every minute.

```
*/1 * * * *   user    ./healthCHeck.sh mail@mail.com http://localhost:8547
```

If you want logs, you can pipe the output to a file such as:

```
*/1 * * * *   user    ./healthCHeck.sh mail@mail.com http://localhost:8547 >> /var/log/parity/health.log
```


