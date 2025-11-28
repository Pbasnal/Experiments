FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Copy csproj and restore dependencies
COPY ["ComicApiOop/ComicApiOop.csproj", "ComicApiOop/"]
RUN dotnet restore "ComicApiOop/ComicApiOop.csproj"

# Install EF Core tools
RUN dotnet tool install --global dotnet-ef
ENV PATH="${PATH}:/root/.dotnet/tools"

# Copy the rest of the code
COPY . .
WORKDIR "/src/ComicApiOop"

# Build and publish
RUN dotnet build "ComicApiOop.csproj" -c Release -o /app/build
RUN dotnet publish "ComicApiOop.csproj" -c Release -o /app/publish

# Build runtime image
FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine
WORKDIR /app
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "ComicApiOop.dll"]