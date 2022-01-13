**免责声明：本项目仅以学习为目的，请在当地法律允许的范围内使用本程序。任何因错误用途导致的法律责任，与本项目无关！**

### awesome_proxy

1. 支持http/https代理
2. 支持socks5代理
3. 混合端口

### 快速开始

#### 1.构建
```
mvn package
```

#### 2.运行
```
cd build

java -Dproxy.username=xxx -Dproxy.password=xxx -Dproxy.port=xxxx -jar proxy.jar
```
参数:
```
proxy.username 用户名
proxy.password 密码
proxy.port 端口
```
