# node-webkit OAuth sample

     +-------------+           +-------------+
     |             | API Call  |   github/   |
     |    xitrum   <----------->   twitter   |
     |             |           |   OAuthAPI  |
     +------+------+           +-------------+
            |        _________/
            |       /
     +------+------+           +-------------+
     |OAuthWindow  |           |MainWindow   |
     |http://      <----------->file://      |
     |  empty.html |postMessage|  index.html |
     +-------------+           +-------------+
                               | node-webkit |
                               +-------------+

