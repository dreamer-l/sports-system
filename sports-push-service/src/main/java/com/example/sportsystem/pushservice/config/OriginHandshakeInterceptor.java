package com.example.sportsystem.pushservice.config;

import com.example.sportsystem.pushservice.service.TokenBlacklistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * WebSocket 握手拦截器
 * 用于鉴权、防止非法连接
 */
@Component
public class OriginHandshakeInterceptor implements HandshakeInterceptor {
    
    private static final String SECRET = "your-secret-key-here"; // 替换为实际密钥
    private static final String KEY_USER = "user";
    // IP白名单（支持通配符*和CIDR格式）
    private List<String> ipWhitelist = List.of("192.168.1.*", "10.0.0.1/24");
    // IP黑名单（支持通配符*和CIDR格式）
    private List<String> ipBlacklist = List.of("192.168.1.100", "10.0.0.5");
    private final TokenBlacklistService tokenBlacklistService;
    // 日志记录器
    private static final Logger log = Logger.getLogger(OriginHandshakeInterceptor.class.getName());

    public OriginHandshakeInterceptor(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * 在握手前拦截请求，进行 Origin 和 Token 验证
     * @param request 请求信息
     * @return 是否允许连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 获取 Token 参数
        String token = request.getURI().getQuery();
        if (token != null && token.contains("token=")) {
            token = token.split("token=")[1].split("&")[0];
        }

        // 获取客户端 IP 地址
        String remoteAddress = request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        // 检查 IP 黑白名单
        if (!isIpAllowed(remoteAddress)) {
            log.warning("[WebSocket] IP 不在允许列表中: " + remoteAddress);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.warning("[WebSocket] Token 已在黑名单中: " + token);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        if (validateToken(token)) {
            attributes.put(KEY_USER, "anonymous_user");
            return true;
        } else {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    /**
     * 握手后处理
     * @param request 请求对象
     * @param response 响应对象
     * @param wsHandler WebSocket处理器
     * @param exception 异常
     */
    /**
     * 验证令牌有效性
     * @param token 待验证的令牌
     * @return 是否有效
     */
    private boolean validateToken(String token) {
        try {
            // 示例实现：验证令牌签名和过期时间
            String[] parts = token.split(Pattern.quote("."));
            if (parts.length != 3) return false;

            long timestamp = Long.parseLong(parts[0]);
            String signature = parts[1];

            // 验证时间戳是否在有效期内（如5分钟内）
            if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
                return false;
            }

            // 生成签名并比较
            String expectedSignature = generateHmacSignature(timestamp);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成 HMAC 签名
     * @param timestamp 时间戳
     * @return 签名字符串
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private String generateHmacSignature(long timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
        mac.init(keySpec);
        byte[] result = mac.doFinal(String.valueOf(timestamp).getBytes());
        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * 检查客户端 IP 是否允许连接
     * @param remoteAddress 客户端 IP 地址
     * @return 是否允许
     */
    private boolean isIpAllowed(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isEmpty()) {
            return false;
        }

        // 检查黑名单
        for (String pattern : ipBlacklist) {
            if (matchIpPattern(remoteAddress, pattern)) {
                return false;
            }
        }

        // 白名单为空表示不限制
        if (ipWhitelist.isEmpty()) {
            return true;
        }

        // 检查是否匹配白名单
        for (String pattern : ipWhitelist) {
            if (matchIpPattern(remoteAddress, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 匹配 IP 地址与模式（支持 CIDR 和通配符）
     * @param ip IP 地址
     * @param pattern 模式（如 192.168.1.*, 10.0.0.1/24）
     * @return 是否匹配
     */
    private boolean matchIpPattern(String ip, String pattern) {
        try {
            if (pattern.contains("/")) {
                // CIDR 格式
                try {
                    InetAddress ipAddress = InetAddress.getByName(ip);
                    InetAddress networkAddress = InetAddress.getByName(pattern.split("/")[0]);
                    int prefixLength = Integer.parseInt(pattern.split("/")[1]);
                            
                    // 计算子网掩码
                    int mask = 0xFFFFFFFF << (32 - prefixLength);
                            
                    // 比较IP地址是否在指定的子网范围内
                    int ipInt = bytesToInt(ipAddress.getAddress());
                    int networkInt = bytesToInt(networkAddress.getAddress());
                            
                    return (ipInt & mask) == (networkInt & mask);
                } catch (Exception e) {
                    return false;
                }
            } else if (pattern.contains("*")) {
                // 通配符格式
                String regex = pattern.replace(".", "\\.").replace("*", ".*");
                return ip.matches(regex);
            } else {
                // 精确匹配
                return ip.equals(pattern);
            }
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 将 CIDR 表达式转换为正则表达式
     * @param cidr CIDR 表达式
     * @return 正则表达式
     */
    private String patternToRegex(String cidr) {
        // 修复非法转义字符问题，使用正确的CIDR匹配逻辑
        String[] parts = cidr.split("/");
        int prefixLength = Integer.parseInt(parts[1]);
        
        // 计算匹配的IP范围
        String ipPart = parts[0];
        int mask = 0xFFFFFFFF << (32 - prefixLength);
        
        // 返回原始CIDR格式，由InetAddress处理验证
        return ipPart + "/" + prefixLength;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 可记录日志或触发其他事件
    }
    
    /**
     * 将字节数组转换为整数
     * @param bytes 字节数组
     * @return 整数值
     */
    private int bytesToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= (bytes[i] & 0xFF) << (8 * (3 - i));
        }
        return value;
    }
}