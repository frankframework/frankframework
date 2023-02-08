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

| Version | Supported          | Security Fixes   | Released         |
| ------- | ------------------ |----------------- |------------------|
| 7.8.x   | :white_check_mark: |:white_check_mark:|                  |
| 7.7.x   | :white_check_mark: |:white_check_mark:| Mar 29, 2022     |
| 7.6.x   | :white_check_mark: |:white_check_mark:| Aug 3, 2021      |
| 7.5.x   | :x:                |:white_check_mark:| Nov 16, 2020     |
| 7.0.x   | :x:                |:x:               | Jun 1, 2018      |
| < 6.1   | :x:                |:x:               | Dec 13, 2016     |


## Reporting a Vulnerability

The Frank! team and our community take security bugs in the Frank!Framework seriously. We appreciate your efforts to 
responsibly disclose your findings, and will make every effort to acknowledge your contributions.

If you would like to report a vulnerability in one of our products, or have security concerns regarding Frank! software, 
please email security@frankframework.org and include the word "SECURITY" in the subject line.

In order for us to best respond to your report, please include any of the following:

* Steps to reproduce or proof-of-concept
* Any relevant tools, including versions used
* Tool output
