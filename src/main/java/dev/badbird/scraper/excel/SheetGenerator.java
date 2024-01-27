package dev.badbird.scraper.excel;

import com.google.gson.*;
import dev.badbird.scraper.objects.Program;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SheetGenerator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @SneakyThrows
    public static void main(String[] args) {
        File top = new File("top_programs.json");
        String json = new String(Files.readAllBytes(top.toPath()));
        JsonObject object = gson.fromJson(json, JsonObject.class);
        List<Program> programs = new ArrayList<>();
        JsonArray arr = object.get("programs").getAsJsonArray();
        for (JsonElement element : arr) {
            JsonObject obj = element.getAsJsonObject();
            programs.add(gson.fromJson(obj, Program.class));
        }
        System.out.println("Total programs: " + programs.size());
        String keywords = object.get("allProgramsData").getAsJsonObject().get("keywords").getAsString();
        System.out.println("Keywords: " + keywords);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(keywords);
        {
            // row 0
            Row info = sheet.createRow(0);
            Cell cell0 = info.createCell(0);
            cell0.setCellValue("School");
            Cell cell1 = info.createCell(1);
            cell1.setCellValue("Program");
            Cell cell2 = info.createCell(2);
            cell2.setCellValue("Requirements");
            // merge 2-5
            CellRangeAddress region = CellRangeAddress.valueOf("C1:E1");
            sheet.addMergedRegion(region);
        }
        int rowNum = 1;
        for (Program program : programs) {
            Row row = sheet.createRow(rowNum++);
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(program.getSchool());
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(program.getName());
            for (int i = 0; i < program.getRequirements().size(); i++) {
                String req = program.getRequirements().get(i);
                Cell cell = row.createCell(i + 2);
                cell.setCellValue(req);
            }
        }

        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream("workbook.xlsx");
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
