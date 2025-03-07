# Гайд по запуску

1. [USVM](https://github.com/oveeernight/usvm), [tac-builder](https://github.com/petrukhinandrew/dotnet-tac), [test-executor](https://github.com/oveeernight/test-executor) должны быть в одной директории
    - `tac-builder` собирается под мак, если это не устраивает, надо сбилдить под целевую платформу
    - при необходимости в `IlMethodTestRunner` поменять платформу в переменной `tacBuilderPath`
2. `samples` сборку можно собрать через таску градла usvm-net/other/dotnet-samples
3. в `IlMethodTestRunner` надо поставить абсолютный путь к `libvsharpCoverage`, который появится при сборке `test-executor`,
   в переменную `profilerPath`

# Написание тестов
1. В usvm-net/src/test/dotnet/samples нужно создать класс с именем `name`, написать метод `method`
и пометить его аттрибутом `SvmTest` 
2. В usvm/net/src/test/samples создать класс с точно тем же именем `name`, добавить метод с тем же
именем `method`, аттрибуты и вызов по аналогии с имеющимися
