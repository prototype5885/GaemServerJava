package org.ProToType.Static;

import java.util.ArrayList;
import java.util.List;

public class ByteProcessor {
    public static void PrintByteArrayAsHex(byte[] byteArray) {
        for (byte b : byteArray) {
            System.out.print(String.format("%02X ", b));
        }
        System.out.println();
    }

    public static List<byte[]> PartitionByteArray(byte[] array, int partitionSize) {
        int numOfPartitions = (int) Math.ceil((double) array.length / partitionSize);
        System.out.println(numOfPartitions);

        List<byte[]> partitions = new ArrayList<>();
        for (int i = 0; i < numOfPartitions; i++) {
            int start = i * partitionSize;
            int end = Math.min(start + partitionSize, array.length);

            int partitionLength = end - start;
            byte[] partition = new byte[partitionLength];
            System.arraycopy(array, start, partition, 0, partitionLength);

            partitions.add(partition);
        }
        return partitions;
    }

    public static byte[] ReconstructByteArray(List<byte[]> chunks) {
        byte[] buffer = new byte[1024];
        int currentIndex = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, buffer, currentIndex, chunk.length);
            currentIndex += chunk.length;
        }
        byte[] reconstructedArray = new byte[currentIndex];
        System.arraycopy(buffer, 0, reconstructedArray, 0, currentIndex);

        return reconstructedArray;
    }
}
