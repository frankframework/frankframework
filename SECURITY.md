# Security Policy

## Security Is Everyone's Responsibility

It is important to remember that the security of your Frank! application is
the result of the overall security of the hosting stack
(*Java*, *Application Server*), Frank!Framework itself, all Java dependencies and
your code. As such, it is your responsibility to follow a few important best
practices:

* **Keep your application up-to-date with the latest Frank!Framework release.** 
By updating your Frank! to the latest version, you ensure that critical vulnerabilities 
are already patched and cannot be exploited in your application.

* **Evaluate your dependencies.** While Maven provides millions of reusable packages,
it is your responsibility to choose trusted 3rd-party libraries. If you use outdated
libraries affected by known vulnerabilities or rely on poorly maintained code,
your application security could be in jeopardy.

* **Adopt secure coding practices.** The first line of defense for your application
is your own code. It is highly recommended to adopt secure software development 
best practices and perform security testing before releasing your application.


## Supported Versions

| Version | Supported                  | Security Fixes*          | Minimal JRE | Released         |
| ------- | -------------------------- | ------------------------ | ----------- | ---------------- |
| latest  | :white_check_mark:         |:white_check_mark:        | Java SE 17  |                  |
| 9.1.x   | :white_check_mark:         |:white_check_mark:        | Java SE 17  | Apr 10, 2025     |
| 9.0.x   | :eight_pointed_black_star: |:white_check_mark:        | Java SE 17  | Jan 06, 2025     |
| 8.3.x   | :white_check_mark:         |:white_check_mark:        | Java SE 17  | Oct 10, 2024     |
| 8.2.x   | :x:                        |:x:                       | Java SE 17  | Jul 11, 2024     |
| 8.1.x   | :x:                        |:x:                       | Java SE 17  | Apr 5, 2024      |
| 8.0.x   | :x:                        |:eight_pointed_black_star:| Java SE 11  | Dec 23, 2023     |
| 7.9.x   | :x:                        |:eight_pointed_black_star:| Java SE 8   | Dec 14, 2023     |
| < 7.8   | :x:                        |:x:                       | Java SE 8   | Apr 5, 2023      |

### Legend

:white_check_mark: Actively supported version, includes performance improvements, bugsfixes and CVE updates  
:x: No longer supported, please upgrade to stay secure  
:eight_pointed_black_star: Only major CVE's and bugs will be patched  


Features will only be added to the latest version. Bug fixes and security updates will happen in the last major and minor versions.  
Please always update to the latest available release. CVE's are solved on a best-effort basis, on versions not older then 1 year after the initial release date (specified in the table above). For more information see our [Security monitoring procedure](https://github.com/frankframework/frankframework/wiki/Security-monitoring-procedure).

## Reporting a Vulnerability

The Frank! team and our community take security bugs in the Frank!Framework seriously. We appreciate your efforts to 
responsibly disclose your findings, and will make every effort to acknowledge your contributions.

If you would like to report a vulnerability in one of our products, or have security concerns regarding Frank! software, 
please email security@frankframework.org and include the word "SECURITY" in the subject line.

In order for us to best respond to your report, please include any of the following:

* Steps to reproduce or proof-of-concept
* Any relevant tools, including versions used
* Tool output

## CI images
In these images, dependencies are updated by dependabot in github. Since we can't test those updates, we have the following process in place. When we are not preparing a release 
(that is, in the last month _before_ release), we can update the dependencies in the CI images. We try to follow the following rules:
* update server versions (like DBMS's) when the client version was successfully updated in the frank framework (eg: when mysql-driver is updated to x.y, we can update mysql-server to x.y as well)
* determine the 'stable' versions before each frank framework release for the ci-images
* update patch versions when available

