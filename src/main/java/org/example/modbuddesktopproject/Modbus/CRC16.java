package org.example.modbuddesktopproject.Modbus;

import java.util.Arrays;

public class CRC16 {
    private CRC16(){};

    public static int calculate(byte[] data){
        //Cria a variavel CRC em 65535 ou 1111 1111 1111 1111 em binario
        // (valor inicial padrao do CRC16 Modbus)
        int crc = 0xFFFF;

        //for para cada byte no array passado
        for (byte b : data) {
            //Transforma o byte de -128 ate 127 para numeros positivos de 0 ate 255
            crc ^= (b & 0xFF);

            //Passa pelo byte
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    //Empurra todos os bits uma casa para a direita
                    crc >>= 1;
                    //Aplica o polinomio CRC16
                    crc ^= 0xA001;
                } else {
                    //So empurra sem passar polinomio
                    crc >>= 1;
                }
            }
        }
        return crc;
    }

    public static boolean validateCRC(byte[] res, int length){
        int receivedCRC =
                //Pensando que o CRC é invertido, então teremos que deixar ele no formato [high, low]
                //Transforma o high byte no tipo que o Java entende e aloca ele 8 bits para a esquerda.
                ((res[length - 1] & 0xFF) << 8)
                        //Transforma o low byte no tipo que o Java entende e preenche os zeros a direita com o valor
                        | (res[length - 2] & 0xFF);
        //pega a resposta e tira o CRC
        byte[] data = Arrays.copyOf(res, length - 2);

        //calcula o CRC internamente
        int calculatedCRC = calculate(data);

        //Ve se o CRC calculado bate com o recebido
        return receivedCRC == calculatedCRC;
    }

    public static boolean isExceptionResponse(byte[] res){
        return (res[1] & 0x80) != 0;
    }

    //Pega o byte inferior
    public static byte getLowByte(int n) {
        return (byte) (n & 0xFF);
    }

    //Pega o byte superior
    public static byte getHighByte(int n) {
        return (byte) ((n >> 8) & 0xFF);
    }
}


