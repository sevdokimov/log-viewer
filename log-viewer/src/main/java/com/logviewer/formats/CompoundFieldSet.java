package com.logviewer.formats;

import com.logviewer.data2.*;
import com.logviewer.formats.utils.LvLayoutNode;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.*;

public class CompoundFieldSet implements FieldSet {

    private final DefaultFieldSet[] fieldSets;

    private final LogFormat.FieldDescriptor[] fields;

    private final Map<String, Integer> fieldNameIndexes;

    private final int[][] fields2mergedFieldsIndexes;

    private final int dateFieldIndex;

    public CompoundFieldSet(@Nullable Locale locale, @Nullable Charset charset, LvLayoutNode[][] layout) {
        fieldSets = new DefaultFieldSet[layout.length];
        for (int i = 0; i < layout.length; i++) {
            fieldSets[i] = new DefaultFieldSet(locale, charset, layout[i]);
        }

        List<LogFormat.FieldDescriptor> mergedFields = new ArrayList<>();
        Map<LogFormat.FieldDescriptor, LogFormat.FieldDescriptor> field2mergedField = new HashMap<>();

        List<LogFormat.FieldDescriptor> usedFields = new ArrayList<>();

        for (DefaultFieldSet fieldSet : fieldSets) {
            usedFields.clear();

            LogFormat.FieldDescriptor[] fields = fieldSet.getFields();

            for (LogFormat.FieldDescriptor field : fields) {
                LogFormat.FieldDescriptor mergedField = null;

                for (LogFormat.FieldDescriptor f : mergedFields) {
                    if (!usedFields.contains(f) && f.name().equals(field.name()) && Objects.equals(f.type(), field.type())) {
                        mergedField = f;
                        break;
                    }
                }

                if (mergedField == null) {
                    mergedField = new DefaultFieldDesciptor(selectUniqueName(mergedFields, field.name()), field.type());
                    mergedFields.add(mergedField);
                }

                usedFields.add(mergedField);
                field2mergedField.put(field, mergedField);
            }
        }

        // Sort merged fields by position weight
        Map<LogFormat.FieldDescriptor, int[]> mergedFieldWeight = new HashMap<>();
        
        for (DefaultFieldSet fieldSet : fieldSets) {
            LogFormat.FieldDescriptor[] fields = fieldSet.getFields();

            for (int i = 0; i < fields.length; i++) {
                LogFormat.FieldDescriptor mergedField = field2mergedField.get(fields[i]);
                assert mergedField != null;
                int[] positionSumAndCount = mergedFieldWeight.computeIfAbsent(mergedField, k -> new int[2]);
                positionSumAndCount[0] += i;
                positionSumAndCount[1]++;
            }
        }

        mergedFields.sort(Comparator.comparingDouble(f -> {
            int[] positionSumAndCount = mergedFieldWeight.get(f);
            return (double) positionSumAndCount[0] / positionSumAndCount[1];
        }));

        fields = mergedFields.toArray(LogFormat.FieldDescriptor.EMPTY_ARRAY);

        fieldNameIndexes = new LinkedHashMap<>();
        for (int i = 0; i < fields.length; i++) {
            LogFormat.FieldDescriptor field = fields[i];
            fieldNameIndexes.put(field.name(), i);
        }

        fields2mergedFieldsIndexes = new int[fieldSets.length][];

        for (int fSetIdx = 0; fSetIdx < fieldSets.length; fSetIdx++) {
            DefaultFieldSet fieldSet = fieldSets[fSetIdx];

            LogFormat.FieldDescriptor[] fields = fieldSet.getFields();

            fields2mergedFieldsIndexes[fSetIdx] = new int[fields.length];

            for (int i = 0; i < fields.length; i++) {
                LogFormat.FieldDescriptor mergedField = field2mergedField.get(fields[i]);
                assert mergedField != null;
                fields2mergedFieldsIndexes[fSetIdx][i] = fieldNameIndexes.get(mergedField.name());
            }
        }

        LogFormat.FieldDescriptor mergedDateField = findCommonDateField(fieldSets, field2mergedField);
        dateFieldIndex = mergedDateField == null ? -1 : fieldNameIndexes.get(mergedDateField.name());
    }

    private static LogFormat.FieldDescriptor findCommonDateField(DefaultFieldSet[] fieldSets,
                                                                 Map<LogFormat.FieldDescriptor, LogFormat.FieldDescriptor> field2mergedField) {
        LogFormat.FieldDescriptor res = null;

        for (DefaultFieldSet fieldSet : fieldSets) {
            LogFormat.FieldDescriptor dateField = fieldSet.getDateField();
            if (dateField == null)
                return null;

            LogFormat.FieldDescriptor mergedField = field2mergedField.get(dateField);
            if (res != null && res != mergedField)
                return null;

            res = mergedField;
        }

        return res;
    }

    @Override
    public LogFormat.FieldDescriptor[] getFields() {
        return fields;
    }

    @Override
    public boolean hasFullDate() {
        return dateFieldIndex >= 0;
    }

    @NonNull
    @Override
    public LogReader createReader() {
        LogReader[] readers = new LogReader[fieldSets.length];

        for (int i = 0; i < fieldSets.length; i++) {
            readers[i] = fieldSets[i].createReader();
        }

        return new CompaundLogReader(readers, (rec, readerIndex) -> {
            int[] mergedFieldPosition = new int[fields.length * 2];
            Arrays.fill(mergedFieldPosition, -1);

            int[] mergedFieldIndexes = fields2mergedFieldsIndexes[readerIndex];
            int[] originalPositions = rec.getFieldPositions();

            assert mergedFieldIndexes.length * 2 == originalPositions.length;

            for (int i = 0; i < mergedFieldIndexes.length; i++) {
                int mergedFieldIdx = mergedFieldIndexes[i];
                mergedFieldPosition[mergedFieldIdx * 2] = originalPositions[i * 2];
                mergedFieldPosition[mergedFieldIdx * 2 + 1] = originalPositions[i * 2 + 1];
            }

            return new LogRecord(rec.getMessage(), rec.getTime(), rec.getStart(), rec.getEnd(), rec.getLoadedTextLengthBytes(),
                    mergedFieldPosition, fieldNameIndexes);
        });
    }

    private static boolean isUniqueName(@NonNull List<LogFormat.FieldDescriptor> fields, @NonNull String baseName) {
        for (LogFormat.FieldDescriptor field : fields) {
            if (field.name().equals(baseName))
                return false;
        }

        return true;
    }

    private static String selectUniqueName(List<LogFormat.FieldDescriptor> fields, String baseName) {
        if (isUniqueName(fields, baseName))
            return baseName;

        int i = 1;
        while (true) {
            String name = baseName + '_' + i;
            if (isUniqueName(fields, name))
                return name;

            i++;
        }
    }
}
