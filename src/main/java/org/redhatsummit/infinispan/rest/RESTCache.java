package org.redhatsummit.infinispan.rest;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.SerializationUtils;

public class RESTCache<K, V> implements ConcurrentMap<K, V> {
    private String cacheName;
    private String baseUrl;

    public RESTCache(String cacheName, String baseUrl) {
        this.cacheName = cacheName;
        this.baseUrl = baseUrl + "/" + cacheName;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsKey(Object key) {
        return false;
    }

    public boolean containsValue(Object value) {
        return false;
    }

    public V get(Object key) {
        String stringKey = toStringKey(key);
        String encodedString = fetch("GET", stringKey, null);
        return (V) decodeToObject(encodedString);
    }

    public V put(K key, V value) {
        String stringKey = toStringKey(key);
        String stringValue = fetch("PUT", stringKey, value);

        return (V) decodeToObject(stringValue);
    }

    public V remove(Object key) {
        String stringKey = toStringKey(key);
        String encodedString = fetch("DELETE", stringKey, null);
        return (V) decodeToObject(encodedString);
    }

    public void putAll(Map<? extends K, ? extends V> map) {

    }

    public void clear() {

    }

    public Set<K> keySet() {
        return null;
    }

    public Collection<V> values() {
        return null;
    }

    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    public V putIfAbsent(K key, V value) {
        return null;
    }

    public boolean remove(Object key, Object value) {
        return false;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    public V replace(K key, V value) {
        return put(key, value);
    }

    private String toStringKey(Object key) {
        if (key instanceof String) {
            return (String) key;
        } else {
            throw new UnsupportedOperationException("RESTful cache only allows String keys!");
        }
    }

    private String fetch(String method, String key, Object value) {
        try {
            URL url = key == null ? new URL(baseUrl) : new URL(baseUrl + "/" + key);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "text/plain");
            byte[] buffer = new byte[2 << 13];

            if(method.equals("PUT")) {
                connection.setDoOutput(true);
                String payload = encodeFromObject((Serializable) value);
                BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
                bos.write(payload.getBytes());
                bos.close();
            }

            connection.connect();
            InputStream responseBodyStream = connection.getInputStream();
            StringBuffer responseBody = new StringBuffer();
            int read = 0;
            while ((read = responseBodyStream.read(buffer)) != -1) {
                responseBody.append(new String(buffer, 0, read));
            }
            connection.disconnect();

            return responseBody.toString();
        } catch (FileNotFoundException fnfe) {
            // Could be that the key being queried does not exist. Return null.
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object decodeToObject(String encodedString) {
        if (encodedString == null || encodedString.length() == 0) {
            return null;
        }
        byte[] objectBytes = Base64.getDecoder().decode(encodedString);
        return SerializationUtils.deserialize(objectBytes);
    }

    private String encodeFromObject(Serializable serializableObject) {
        byte[] objectBytes = SerializationUtils.serialize(serializableObject);
        byte[] encodedObjectBytes = Base64.getEncoder().encode(objectBytes);
        return new String(encodedObjectBytes);
    }
}
