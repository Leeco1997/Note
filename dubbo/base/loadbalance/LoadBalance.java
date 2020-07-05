package base.loadbalance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author liqiao
 * @date 2020/7/1 16:43
 * @description 负载均衡
 */


public class LoadBalance {
    public static void main(String[] args) {
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("Node A", 2);
        hashMap.put("Node B", 7);
        hashMap.put("Node C", 1);
        for (int i = 0;i<20;i++){
            new LoadBalance().weightRound2(hashMap);
        }
    }

    /**
     * 权重随机
     */
    private void weightRound1(HashMap<String, Integer> hashMap) {
        //使用list存储，浪费空间
        ArrayList<String> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                list.add(entry.getKey());
            }
        }
        int allWeight = hashMap.values().stream().mapToInt(a -> a).sum();
        int number = new Random().nextInt(allWeight);
        System.out.println(list.get(number));
    }
    /**
     * 如果A服务器的权重是2，B服务器的权重是7，C服务器的权重是1：
     * 如果我生成的随机数是1，那么落到A服务器，因为1<=2（A服务器的权重）
     * 如果我生成的随机数是5，那么落到B服务器，因为5>2（A服务器的权重），5-2（A服务器的权重）=3，3<7（B服务器的权重）
     * 如果我生成的随机数是10，那么落到C服务器，因为10>2（A服务器的权重），10-2（A服务器的权重）=8，8>7（B服务器的权重），8-7（B服务器的权重）=1，
     * 1<=1（C服务器的权重）
     */
    private void weightRound2(HashMap<String, Integer> hashMap) {
        //使用分段的思想
        int allWeight = hashMap.values().stream().mapToInt(a -> a).sum();
        int number = new Random().nextInt(allWeight);
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            if (number <= entry.getValue()) {
                System.out.println(entry.getKey());
            } else {
                number -= entry.getValue();
            }
        }
    }

    //todo 平滑加权算法
    //todo  一致性hash算法，treeMap


}
