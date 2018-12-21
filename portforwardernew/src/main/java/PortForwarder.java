import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class PortForwarder {

    private static int iPort;
    private static int rPort;
    private static String rHost;
    private static int bufferSize = 10000;


    static class Attachment {
        ByteBuffer in;
        ByteBuffer out;
        SelectionKey pairKey;
        boolean inIsRead;

        public Attachment(SelectionKey pairKey) {
            this.in = ByteBuffer.allocate(bufferSize);
            this.out = ByteBuffer.allocate(bufferSize);
            this.pairKey = pairKey;
            this.inIsRead = true;
        }
    }

    public static void main(String[] args) throws IOException {  //0 - наш порт, 1 - recv порт , 2 - recv имя
        String myError;
        if (args.length < 3) {
            myError = ErrorHandler.getInstance().getError(1);
            System.out.println(myError);
            System.exit(1);
        }
        if ((myError = ErrorHandler.getInstance().isValidPort(Integer.valueOf(args[0]))).equals("Success"))
            iPort = Integer.parseInt(args[0]);
        else {
            System.err.println(myError);
            System.exit(2);
        }
        if ((myError = ErrorHandler.getInstance().isValidPort(Integer.valueOf(args[1]))).equals("Success"))
            rPort = Integer.parseInt(args[1]);
        else {
            System.err.println(myError);
            System.exit(2);
        }
        if ((myError = ErrorHandler.getInstance().resolveHostName(args[2])).equals("Success"))
            rHost = args[2];
        else {
            System.err.println(myError);
            System.exit(3);
        }

        Selector selector = Selector.open();
        //создание канала
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        //привязываем канал к порту
        ssc.socket().bind(new InetSocketAddress(iPort));
        ssc.register(selector, SelectionKey.OP_ACCEPT); //SelectionKey.OP_ACCEPT - хотим только входящие соединения

        while (true) {
            int count = selector.select(); //принятие всех событий
//            System.out.println("select " + count);

            if (count < 0)
                continue;
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); //ключи, где что-то произошло
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // проверим валидность нашего ключа --
                // ключ ялвяется валидным до тех пор, пока не будет отменен, или его канал не будет закрыт,
                // или его селектор не будет закрыт
                if (key.isValid()) {
                    try {
                        // далее -- проверим, к какому событию готов канал
                        // готовность канала определяется вторым аргументом register
                        if (key.isAcceptable()) { //принимаем соединение с удаленного сервера
                            System.out.println("have new accept " + key);
                            accept(key); //запрос на соединение
                        } else if (key.isReadable()) { //канал готов к чтению
                            //System.out.println("Have data to read "+key);
                            read(key);
                        } else if (key.isWritable()) { //готов к записи
                            //System.out.println("Have data to write "+key);
                            write(key);
                        } else if (key.isConnectable()) { //запрос удаленного ресурса -- могу ли я получить доступ
                            connect(key);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        close(key);
                    }
                }
                iterator.remove();
            }
        }
    }

    private static void accept(SelectionKey key) throws IOException { //создаем канал для клиента и его в селектор
        SocketChannel newClientChannel = ((ServerSocketChannel) key.channel()).accept();  //подключаем клиентов
        newClientChannel.configureBlocking(false);
        SelectionKey clientKey = newClientChannel.register(key.selector(), 0); //будем слушать, когда передает и читает данные

        //работа с сервером
        SocketChannel newServerChannel = SocketChannel.open();
        newServerChannel.configureBlocking(false);
        newServerChannel.connect(new InetSocketAddress(InetAddress.getByName(rHost), rPort));
        SelectionKey serverKey = newServerChannel.register(key.selector(), SelectionKey.OP_CONNECT);

        Attachment serverAttachment = new Attachment(clientKey);
        serverKey.attach(serverAttachment);
        Attachment clientAttachment = new Attachment(serverKey);
        clientKey.attach(clientAttachment);

        System.out.println("clientKey = " + clientKey);
        System.out.println("serverKey = " + serverKey);
    }

    private static void connect(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());

        if (channel.finishConnect()) {
            attachment.out = ((Attachment) attachment.pairKey.attachment()).in;
            ((Attachment) attachment.pairKey.attachment()).out = attachment.in;
            attachment.pairKey.interestOps(SelectionKey.OP_READ);
            key.interestOps(SelectionKey.OP_READ);
        }
        else{
            close(key);
        }
    }

    private static void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Attachment attachment = ((Attachment) key.attachment());
        int count = channel.read(attachment.in);
        if (count > 1) {
            System.out.println("was read " + count + " from " + key);
            attachment.inIsRead = false;
            attachment.pairKey.interestOps(attachment.pairKey.interestOps() | SelectionKey.OP_WRITE); //говорим, чтобы второй конец принимал данные
            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ); //убираем интерес на передачу данных у сработавшего соединения
            attachment.in.flip(); //готовим буффер для записи
        } else {
            close(key);
        }
    }

    private static void write(SelectionKey key) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        Attachment attachment = ((Attachment) key.attachment());

        int count = channel.write(attachment.out);
        if (count > 0) { //успешно прочитали данные
            if(attachment.pairKey == null){ //если "соседа" нет, то закрываем соединение
                close(key);
            }
            attachment.out.compact();
            System.out.println("was write " + count + " to " + key);
            attachment.pairKey.interestOps(attachment.pairKey.interestOps() | SelectionKey.OP_READ);// Добавялем ко второму концу интерес на чтение
            if (attachment.out.hasRemaining()) {
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE); // у своего убираем интерес на запись
            }
        } else {
            close(key);
        }
    }

    private static void close(SelectionKey key) throws IOException {
        key.channel().close();
        key.cancel();
        SelectionKey pairKey = ((Attachment) key.attachment()).pairKey;

        if(pairKey != null) {
            ((Attachment) pairKey.attachment()).pairKey = null;
//            pairKey.interestOps(SelectionKey.OP_WRITE);
        }
    }
}