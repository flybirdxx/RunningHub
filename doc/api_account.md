# 获取账户信息 API

## OpenAPI Specification

```yaml
openapi: 3.0.1
info:
  title: 'RunningHub Account Info'
  description: 'API to fetch user account information including coins and task counts.'
  version: 1.0.0
paths:
  /uc/openapi/accountStatus:
    post:
      summary: 获取账户信息
      parameters:
        - name: Host
          in: header
          required: true
          example: www.runninghub.cn
          schema:
            type: string
        - name: Authorization
          in: header
          required: true
          example: Bearer [Your API KEY]
          schema:
            type: string
            default: Bearer [Your API KEY]
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                apikey:
                  type: string
            example:
              apikey: '{{apiKey}}'
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                type: object
                properties:
                  code:
                    type: integer
                  msg:
                    type: string
                  data:
                    type: object
                    properties:
                      remainCoins:
                        type: string
                      currentTaskCounts:
                        type: string
                      remainMoney:
                        type: string
                        nullable: true
                      currency:
                        type: string
                        nullable: true
                      apiType:
                        type: string
                required:
                  - code
                  - msg
                  - data
```
