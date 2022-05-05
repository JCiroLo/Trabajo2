
import multichain.command.*;
import multichain.object.*;
import java.util.List;
import java.util.Scanner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class Main {

    public static String votoToJson(String voto) {
        return "{\"json\":{\"voto\":\"" + voto + "\"}}";
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        CommandManager commandManager = new CommandManager(
                "localhost",
                "4760",
                "multichainrpc",
                "cZPFbBAmXaqZVx3QrT9PkyTEJAf5k94Ybidcn5zQi62"
        );

        List<StreamKeyItem> candidatos = new ArrayList<>();
        
        try {
            commandManager.invoke(CommandElt.SUBSCRIBE, "Candidatos");
            candidatos = (List<StreamKeyItem>) commandManager.invoke(CommandElt.LISTSTREAMITEMS, "Candidatos");
        } catch (MultichainException e) {
            System.out.println("Error al obtener candidatos");
        }

        int choice = 0;

        while (choice != 3) {
            System.out.println("");
            System.out.println("Por favor seleccione una opción:");
            System.out.println("1. Votar por candidato");
            System.out.println("2. Ver resultados de los candidatos");
            System.out.println("3. Salir");
            System.out.println("");

            choice = input.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("");
                    System.out.println("Los candidatos son:");
                    System.out.println("");

                    for (StreamKeyItem item : candidatos) {
                        String json = item.getData().toString().replaceAll("\\s+", "");
                        JsonObject data = new JsonParser().parse(json).getAsJsonObject();
                        String nombre = data.get("json").getAsJsonObject().get("nombre").getAsString();
                        String partido = data.get("json").getAsJsonObject().get("partido").getAsString();

                        System.out.println("Candidato: " + item.getKeys().get(0));
                        System.out.println("Nombre: " + nombre);
                        System.out.println("Partido: " + partido);
                        System.out.println("");
                    }

                    System.out.println("Ingrese su documento de identificación:");

                    String userId = input.next();

                    System.out.println("");
                    System.out.println("Ingrese el número del candidato por quien desea votar:");

                    String candidato = input.next();

                    System.out.println("");

                    List<StreamKeyItem> candidatoValido = new ArrayList<>();

                    try {
                        candidatoValido = (List<StreamKeyItem>) commandManager.invoke(CommandElt.LISTSTREAMKEYITEMS, "Candidatos", candidato);
                    } catch (MultichainException e) {
                        System.out.println("Error al validar candidato");
                    }

                    if (candidatoValido.isEmpty()) {
                        System.out.println("Error: Candidato no existe");
                    } else {
                        try {
                            JsonObject json = new JsonParser().parse(votoToJson(candidato)).getAsJsonObject();
                            commandManager.invoke(CommandElt.PUBLISH, "Votos", userId, json);
                        } catch (MultichainException e) {
                            System.out.println("Error al crear voto");
                            System.out.println("");
                        }
                    }
                    break;
                case 2:
                    System.out.println("");
                    try {
                        commandManager.invoke(CommandElt.SUBSCRIBE, "Votos");
                        List<StreamKeyItem> votos = (List<StreamKeyItem>) commandManager.invoke(CommandElt.LISTSTREAMITEMS, "Votos");
                        Dictionary<String, Integer> resultados = new Hashtable();

                        for (StreamKeyItem c : candidatos) {
                            resultados.put(c.getKeys().get(0), 0);
                        }

                        for (StreamKeyItem voto : votos) {
                            try {
                                String json = voto.getData().toString().replaceAll("\\s+", "");
                                JsonObject data = new JsonParser().parse(json).getAsJsonObject();
                                String votoCandId = data.get("json").getAsJsonObject().get("voto").getAsString();
                                resultados.put(votoCandId, resultados.get(votoCandId) + 1);
                            } catch (Exception e) {
                            }

                        }

                        for (StreamKeyItem c : candidatos) {
                            String json = c.getData().toString().replaceAll("\\s+", "");
                            JsonObject data = new JsonParser().parse(json).getAsJsonObject();
                            String nombreCandidato = data.get("json").getAsJsonObject().get("nombre").getAsString();
                            String partidoCandidato = data.get("json").getAsJsonObject().get("partido").getAsString();

                            Integer res = resultados.get(c.getKeys().get(0));
                            System.out.println(nombreCandidato + "(" + partidoCandidato + "): " + res + " votos");
                        }

                    } catch (MultichainException e) {
                        System.out.println("Error al obtener los resultados");
                    }
                    break;
            }
        }
    }
}
