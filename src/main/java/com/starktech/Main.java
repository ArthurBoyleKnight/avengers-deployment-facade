package com.starktech;

import com.starktech.facade.AdminFacade;
import com.starktech.facade.DeploymentFacade;
import com.starktech.facade.DeploymentFailedException;
import com.starktech.facade.DeploymentReport;
import com.starktech.workflow.ChainRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        DeploymentFacade deployment =
            new DeploymentFacade(Path.of("data"));

        AdminFacade admin = new AdminFacade(deployment);

        while (true) {
            System.out.println("\n=== STARK TECH MISSION SYSTEM ===");
            System.out.println("1. Deploy");
            System.out.println("2. Deploy with forced failure at diagnostics (B)");
            System.out.println("3. Deploy with forced failure at clearance (C)");
            System.out.println("4. Inspect deployment");
            System.out.println("5. Compensate completed deployment");
            System.out.println("6. List all deployments");
            System.out.println("7. Recover interrupted deployments");
            System.out.println("8. Exit");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1", "2", "3" -> {
                        System.out.print("Drone ID (e.g. DRONE-01, DRONE-01-LOWBAT): ");
                        String droneId = scanner.nextLine().trim();

                        System.out.print("Mission code (e.g. M-AVENGER7, M-AVENGER7-DENIED): ");
                        String missionCode = scanner.nextLine().trim();

                        String id = deployment.deploy(
                            droneId,
                            missionCode,
                            choice.equals("2"),
                            choice.equals("3")
                        );

                        System.out.println(
                            "Deployment completed. Chain ID: " + id
                        );
                    }

                    case "4" -> {
                        System.out.print("Chain ID: ");
                        String id = scanner.nextLine().trim();

                        DeploymentReport report = admin.report(id);

                        System.out.println("ID: " + report.id());
                        System.out.println("Chain status: " + report.chain());
                        System.out.println("Inventory (A): " + report.inventory());
                        System.out.println("Diagnostics (B): " + report.diagnostics());
                        System.out.println("Clearance (C): " + report.clearance());
                        System.out.println("Details: " + report.detail());
                    }

                    case "5" -> {
                        System.out.print("Chain ID: ");
                        String id = scanner.nextLine().trim();

                        admin.compensate(id);
                        System.out.println("Compensation completed.");
                    }

                    case "6" -> {
                        List<ChainRecord> chains = admin.list();

                        if (chains.isEmpty()) {
                            System.out.println("No deployments stored.");
                        }

                        for (ChainRecord chain : chains) {
                            System.out.println(
                                chain.getId() + "  " + chain.getStatus()
                                + "  " + chain.getDetail()
                            );
                        }
                    }

                    case "7" -> {
                        int recovered = admin.recoverInterrupted();
                        System.out.println(
                            "Recovered interrupted deployments: " + recovered
                        );
                    }

                    case "8" -> {
                        System.out.println("Goodbye!");
                        scanner.close();
                        return;
                    }

                    default ->
                        System.out.println("Invalid choice.");
                }
            } catch (DeploymentFailedException e) {
                System.out.println(e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
